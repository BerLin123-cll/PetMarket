package com.pet.petmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.petmarket.dto.CreateOrderDTO;
import com.pet.petmarket.entity.*;
import com.pet.petmarket.mapper.*;
import com.pet.petmarket.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存与订单一致性验收测试（P0-3）
 *
 * <p>覆盖点：</p>
 * <ol>
 *   <li>下单原子扣减库存（条件扣减），库存不足即整单失败且库存不变；</li>
 *   <li>并发下单不会超卖（8 个线程抢 2 件库存，只能成功 2 单）；</li>
 *   <li>取消订单回补库存，且重复取消不会重复回补（幂等）；</li>
 *   <li>超时未支付订单被自动取消并释放库存，且释放幂等；</li>
 *   <li>已支付订单不会被超时任务误释放；</li>
 *   <li>支付/发货的状态机校验与防重复支付。</li>
 * </ol>
 *
 * <p>使用 H2 内存库（MySQL 兼容模式）+ 测试专用建表脚本，不依赖本机 MySQL。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_inventory;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=20000"
})
class OrderInventoryConsistencyTest {

    private static final Long USER_ID = 1L;
    private static final Long ADDRESS_ID = 1L;

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private AddressMapper addressMapper;

    private Long productId;

    @BeforeEach
    void setUp() {
        // 清理上一轮测试数据，保证每个用例起点一致（并发用例需要数据已提交，故不使用 @Transactional）
        ordersMapper.delete(null);
        orderItemMapper.delete(null);
        cartMapper.delete(null);
        productMapper.delete(null);
        // 保留 schema 中 id=1 的基准地址，清掉并发用例产生的临时地址
        addressMapper.delete(new LambdaQueryWrapper<Address>().gt(Address::getId, ADDRESS_ID));

        Product product = new Product();
        product.setName("测试狗粮");
        product.setCategoryId(1L);
        product.setMainImage("/images/product/dog-food.jpg");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(2);
        product.setStatus(1);
        productMapper.insert(product);
        productId = product.getId();
    }

    private Long addCartItem(Long userId, int quantity) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cartMapper.insert(cart);
        return cart.getId();
    }

    private CreateOrderDTO orderDto(Long cartId) {
        return orderDto(cartId, ADDRESS_ID);
    }

    private CreateOrderDTO orderDto(Long cartId, Long addressId) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setAddressId(addressId);
        dto.setCartIds(List.of(cartId));
        return dto;
    }

    /** 为指定用户新建一条收货地址（下单会校验地址归属，故并发用例需各自独立地址） */
    private Long addAddressFor(Long userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName("并发用户" + userId);
        address.setPhone("13800138000");
        address.setDetailAddress("北京市朝阳区并发测试路" + userId + "号");
        address.setIsDefault(0);
        addressMapper.insert(address);
        return address.getId();
    }

    private int stockOf() {
        return productMapper.selectById(productId).getStock();
    }

    // ---------------------------------------------------------------
    // 1. 正常下单：原子扣减库存
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收1：下单成功按数量扣减库存，订单为待支付并带支付截止时间")
    void createOrderDeductsStock() {
        Long cartId = addCartItem(USER_ID, 2);

        OrderVO order = ordersService.createOrder(USER_ID, orderDto(cartId));

        assertNotNull(order.getId());
        assertEquals(0, stockOf(), "库存应从 2 扣减为 0");
        assertEquals(0, order.getOrderStatus(), "新订单应为待支付");
        assertEquals(0, order.getPayStatus());
        assertEquals(new BigDecimal("200.00"), order.getTotalAmount());
        assertNotNull(order.getPayExpireTime(), "应写入支付截止时间，供超时任务使用");
        assertTrue(order.getPayExpireTime().isAfter(LocalDateTime.now()), "支付截止时间应在当前时间之后");
        assertTrue(cartMapper.selectById(cartId) == null, "下单后购物车项应被清空");
        assertEquals(1, orderItemMapper.selectList(null).size(), "应生成 1 条订单项");
    }

    // ---------------------------------------------------------------
    // 2. 库存不足：整单失败且库存不变（不会扣一半）
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收2：库存不足时下单失败，库存保持不变")
    void createOrderFailsWhenStockInsufficient() {
        Long cartId = addCartItem(USER_ID, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ordersService.createOrder(USER_ID, orderDto(cartId)));

        assertTrue(ex.getMessage().contains("库存不足"), ex.getMessage());
        assertEquals(2, stockOf(), "下单失败不应改动库存");
        assertEquals(0, ordersMapper.selectList(null).size(), "下单失败不应产生订单");
        assertNotNull(cartMapper.selectById(cartId), "下单失败购物车项应保留");
    }

    // ---------------------------------------------------------------
    // 3. 并发下单：不超卖
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收3：8 个并发请求抢 2 件库存，恰好成功 2 单，不超卖")
    void concurrentOrdersDoNotOversell() throws Exception {
        int threads = 8;
        int stock = stockOf();

        List<Long> cartIds = new ArrayList<>();
        List<Long> addressIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            // 每个线程独立的用户 + 收货地址 + 购物车项，避免互相影响
            Long userId = 1000L + i;
            addressIds.add(addAddressFor(userId));
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(1);
            cartMapper.insert(cart);
            cartIds.add(cart.getId());
        }

        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    CreateOrderDTO dto = orderDto(cartIds.get(index), addressIds.get(index));
                    ordersService.createOrder(1000L + index, dto);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
                return null;
            }));
        }
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(stock, success.get(), "成功下单数必须等于库存数（不超卖）");
        assertEquals(threads - stock, failed.get(), "其余请求应因库存不足失败");
        assertEquals(0, stockOf(), "最终库存应恰好为 0，不能为负");
        assertEquals(stock, ordersMapper.selectList(null).size(), "数据库中只应有成功下单的订单");
    }

    // ---------------------------------------------------------------
    // 4. 取消订单：回补库存且幂等
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收4：取消订单回补库存，重复取消不会重复回补")
    void cancelOrderRestoresStockIdempotently() {
        Long cartId = addCartItem(USER_ID, 2);
        OrderVO order = ordersService.createOrder(USER_ID, orderDto(cartId));
        assertEquals(0, stockOf());

        ordersService.cancelOrder(USER_ID, order.getId());
        assertEquals(2, stockOf(), "取消后库存应回补到 2");
        assertEquals(4, ordersMapper.selectById(order.getId()).getOrderStatus(), "订单状态应为已取消");

        // 再次取消：应被状态校验拦下，且库存不被重复回补
        assertThrows(RuntimeException.class, () -> ordersService.cancelOrder(USER_ID, order.getId()));
        assertEquals(2, stockOf(), "重复取消不得重复回补库存");
    }

    // ---------------------------------------------------------------
    // 5. 超时自动取消：释放库存且幂等
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收5：超时未支付订单被自动取消并释放库存，重复释放幂等")
    void timeoutOrderIsReleasedIdempotently() {
        Long cartId = addCartItem(USER_ID, 2);
        OrderVO order = ordersService.createOrder(USER_ID, orderDto(cartId));

        // 把创建时间改到超时之前，模拟「下单后一直未支付」
        LambdaUpdateWrapper<Orders> backdate = new LambdaUpdateWrapper<>();
        backdate.eq(Orders::getId, order.getId())
                .set(Orders::getCreateTime, LocalDateTime.now().minusMinutes(40));
        ordersMapper.update(null, backdate);

        int released = ordersService.releaseTimeoutOrders(10);

        assertEquals(1, released, "应释放 1 单超时订单");
        assertEquals(4, ordersMapper.selectById(order.getId()).getOrderStatus(), "超时订单应变为已取消");
        assertEquals(2, stockOf(), "超时释放后库存应回补到 2");

        // 幂等性：再次扫描 / 再次释放同一订单都不应重复回补
        assertEquals(0, ordersService.releaseTimeoutOrders(10), "已取消订单不应被再次释放");
        assertFalse(ordersService.releaseTimeoutOrder(order.getId()), "同一订单重复释放应返回 false");
        assertEquals(2, stockOf(), "重复释放不得重复回补库存");
    }

    // ---------------------------------------------------------------
    // 6. 未超时订单不被释放
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收6：未超过支付时限的订单不会被定时任务取消")
    void unpaidOrderWithinTimeoutIsKept() {
        Long cartId = addCartItem(USER_ID, 1);
        OrderVO order = ordersService.createOrder(USER_ID, orderDto(cartId));

        assertEquals(0, ordersService.releaseTimeoutOrders(10), "未超时订单不应被释放");
        assertEquals(0, ordersMapper.selectById(order.getId()).getOrderStatus(), "订单应仍为待支付");
        assertEquals(1, stockOf(), "库存应保持被预占状态");
    }

    // ---------------------------------------------------------------
    // 7. 已支付订单不被超时任务误伤 + 防重复支付
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收7：已支付订单不会被超时任务释放，且不能重复支付")
    void paidOrderIsNotReleasedByTimeoutTask() {
        Long cartId = addCartItem(USER_ID, 1);
        OrderVO order = ordersService.createOrder(USER_ID, orderDto(cartId));

        ordersService.payOrder(USER_ID, order.getOrderNo());
        Orders paid = ordersMapper.selectById(order.getId());
        assertEquals(1, paid.getOrderStatus());
        assertEquals(1, paid.getPayStatus());
        assertNotNull(paid.getPayTime());

        // 即使把创建时间改到很早，已支付订单也不该被释放
        LambdaUpdateWrapper<Orders> backdate = new LambdaUpdateWrapper<>();
        backdate.eq(Orders::getId, order.getId())
                .set(Orders::getCreateTime, LocalDateTime.now().minusMinutes(120));
        ordersMapper.update(null, backdate);

        assertEquals(0, ordersService.releaseTimeoutOrders(10), "已支付订单不应被释放");
        assertFalse(ordersService.releaseTimeoutOrder(order.getId()), "已支付订单不应被释放");
        // 下单预占 1 件（2 → 1），支付成功后库存不应被回补，仍为 1
        assertEquals(1, stockOf(), "已支付订单不应回补库存");

        // 重复支付应失败
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ordersService.payOrder(USER_ID, order.getOrderNo()));
        assertTrue(ex.getMessage().contains("状态"), ex.getMessage());

        // 发货状态机：已支付才能发货
        ordersService.shipOrder(order.getId());
        assertEquals(2, ordersMapper.selectById(order.getId()).getOrderStatus());
        assertThrows(RuntimeException.class, () -> ordersService.shipOrder(order.getId()),
                "已发货订单不能重复发货");
    }

    // ---------------------------------------------------------------
    // 8. 订单列表与详情仍可正常读取（回归）
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收8：订单列表/详情返回正确的订单项与金额（回归验证）")
    void orderQueryStillWorks() {
        Long cartId = addCartItem(USER_ID, 1);
        OrderVO created = ordersService.createOrder(USER_ID, orderDto(cartId));

        OrderVO detail = ordersService.getOrderDetail(USER_ID, created.getId());
        assertEquals(1, detail.getItems().size());
        assertEquals(new BigDecimal("100.00"), detail.getItems().get(0).getSubtotal());
        assertEquals("测试狗粮", detail.getItems().get(0).getProductName());

        IPage<OrderVO> page = ordersService.getUserOrders(USER_ID, 1, 10, null);
        assertEquals(1, page.getTotal());

        // 越权访问他人订单应失败
        assertThrows(RuntimeException.class, () -> ordersService.getOrderDetail(999L, created.getId()));
    }
}
