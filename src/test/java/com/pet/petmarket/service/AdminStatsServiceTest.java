package com.pet.petmarket.service;

import com.pet.petmarket.entity.OrderItem;
import com.pet.petmarket.entity.Orders;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.mapper.OrderItemMapper;
import com.pet.petmarket.mapper.OrdersMapper;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.vo.HotProductVO;
import com.pet.petmarket.vo.LowStockProductVO;
import com.pet.petmarket.vo.OrderStatusCountVO;
import com.pet.petmarket.vo.RepurchaseVO;
import com.pet.petmarket.vo.SalesTrendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 后台数据概览统计服务测试（P2 管理端）
 *
 * <p>覆盖 5 类经营洞察数据的口径与边界：</p>
 * <ol>
 *   <li>销售趋势：按天聚合已支付订单、缺失日期补 0；</li>
 *   <li>订单状态漏斗：补齐全部状态并写中文状态名；</li>
 *   <li>热销商品：仅统计已支付订单、按销量降序；</li>
 *   <li>低库存预警：仅上架商品、库存升序；</li>
 *   <li>用户复购率：≥2 笔已支付占比，无买家时返回 0 不报错。</li>
 * </ol>
 *
 * <p>使用独立 H2 内存库（MySQL 兼容模式），不依赖本机 MySQL。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_stats;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class AdminStatsServiceTest {

    /** H2 基础数据里的分类ID（schema-h2.sql 中 category id=1） */
    private static final Long CATEGORY_ID = 1L;

    @Autowired
    private AdminStatsService adminStatsService;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper.delete(null);
        ordersMapper.delete(null);
        productMapper.delete(null);
    }

    // ---------------------------------------------------------------
    // 测试辅助
    // ---------------------------------------------------------------

    private Long insertOrder(Long userId, int orderStatus, int payStatus,
                             BigDecimal amount, LocalDateTime payTime) {
        Orders order = new Orders();
        order.setOrderNo("ORD" + System.nanoTime() + "-" + userId);
        order.setUserId(userId);
        order.setTotalAmount(amount);
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800138000");
        order.setReceiverAddress("测试地址");
        order.setOrderStatus(orderStatus);
        order.setPayStatus(payStatus);
        order.setPayTime(payTime);
        ordersMapper.insert(order);
        return order.getId();
    }

    private void insertItem(Long orderId, Long productId, String name, String price, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(name);
        item.setPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        orderItemMapper.insert(item);
    }

    private Long insertProduct(String name, int stock, int status) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryId(CATEGORY_ID);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(stock);
        product.setStatus(status);
        productMapper.insert(product);
        return product.getId();
    }

    private static BigDecimal amountOf(List<SalesTrendVO> list, LocalDate date) {
        return list.stream()
                .filter(point -> date.equals(point.getSaleDate()))
                .map(SalesTrendVO::getAmount)
                .findFirst()
                .orElseThrow(() -> new AssertionError("趋势缺少日期：" + date));
    }

    // ---------------------------------------------------------------
    // 1. 销售趋势：按天聚合 + 补 0
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收1：销售趋势按天聚合已支付订单，缺失日期补 0")
    void salesTrendAggregatesByDay() {
        LocalDate today = LocalDate.now();
        // 今天两笔已支付订单：100 + 50 = 150
        insertOrder(1L, 1, 1, new BigDecimal("100.00"), today.atTime(10, 0));
        insertOrder(2L, 1, 1, new BigDecimal("50.00"), today.atTime(15, 0));
        // 3 天前一笔已支付：200
        insertOrder(3L, 1, 1, new BigDecimal("200.00"), today.minusDays(3).atTime(9, 0));
        // 未支付订单不应被统计
        insertOrder(4L, 0, 0, new BigDecimal("999.00"), null);

        List<SalesTrendVO> trend = adminStatsService.getSalesTrend(7);

        assertEquals(7, trend.size(), "应返回连续 7 天");
        assertEquals(LocalDate.now().minusDays(6), trend.get(0).getSaleDate(), "首日应为 6 天前");
        assertEquals(0, new BigDecimal("150.00").compareTo(amountOf(trend, today)), "今天应为 150");
        assertEquals(0, new BigDecimal("200.00").compareTo(amountOf(trend, today.minusDays(3))), "3 天前应为 200");
        assertEquals(0, BigDecimal.ZERO.compareTo(amountOf(trend, today.minusDays(1))), "无数据日期应补 0");
    }

    // ---------------------------------------------------------------
    // 2. 订单状态漏斗：补齐状态
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收2：订单状态漏斗补齐全部状态并写中文名")
    void orderStatusFunnelFillsAllStatuses() {
        insertOrder(1L, 0, 0, new BigDecimal("10.00"), null);
        insertOrder(2L, 0, 0, new BigDecimal("10.00"), null);
        insertOrder(3L, 1, 1, new BigDecimal("10.00"), LocalDateTime.now());
        insertOrder(4L, 4, 0, new BigDecimal("10.00"), null);

        List<OrderStatusCountVO> funnel = adminStatsService.getOrderStatusFunnel();

        assertEquals(5, funnel.size(), "应返回全部 5 个状态");
        assertEquals("待支付", funnel.get(0).getStatusText());
        assertEquals(2L, funnel.get(0).getCount());
        assertEquals(1L, funnel.get(1).getCount(), "已支付应为 1");
        assertEquals(0L, funnel.get(2).getCount(), "已发货应补 0");
        assertEquals(0L, funnel.get(3).getCount(), "已完成应补 0");
        assertEquals(1L, funnel.get(4).getCount(), "已取消应为 1");
    }

    // ---------------------------------------------------------------
    // 3. 热销商品：仅已支付、按销量降序
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收3：热销商品仅统计已支付订单并按销量降序")
    void hotProductsOnlyCountPaidAndSortByQuantity() {
        Long paidOrder = insertOrder(1L, 1, 1, new BigDecimal("110.00"), LocalDateTime.now());
        insertItem(paidOrder, 1L, "狗粮A", "10.00", 5);
        insertItem(paidOrder, 2L, "猫粮B", "20.00", 3);

        // 未支付订单里的商品不应计入
        Long unpaidOrder = insertOrder(2L, 0, 0, new BigDecimal("1000.00"), null);
        insertItem(unpaidOrder, 3L, "爆款C", "100.00", 100);

        List<HotProductVO> hot = adminStatsService.getHotProducts(5);

        assertEquals(2, hot.size(), "未支付订单的商品应被排除");
        assertEquals("狗粮A", hot.get(0).getProductName(), "销量高的排前面");
        assertEquals(5L, hot.get(0).getQuantity());
        assertEquals(0, new BigDecimal("50.00").compareTo(hot.get(0).getAmount()), "销售额=单价×数量");
    }

    // ---------------------------------------------------------------
    // 4. 低库存预警：仅上架、库存升序
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收4：低库存预警仅上架商品且库存升序")
    void lowStockOnlyOnShelfAndSortedAsc() {
        insertProduct("低库存5", 5, 1);
        insertProduct("低库存3", 3, 1);
        insertProduct("库存充足", 50, 1);
        insertProduct("已下架1", 1, 0);

        List<LowStockProductVO> list = adminStatsService.getLowStockProducts(10, 10);

        assertEquals(2, list.size(), "仅返回库存≤10 且上架的商品");
        assertEquals("低库存3", list.get(0).getName(), "库存最少的排前面");
        assertEquals(3, list.get(0).getStock());
        assertEquals("低库存5", list.get(1).getName());
    }

    // ---------------------------------------------------------------
    // 5. 用户复购率
    // ---------------------------------------------------------------
    @Test
    @DisplayName("验收5：复购率=下单≥2笔用户 / 有支付用户，且无买家返回0")
    void repurchaseRateCalculation() {
        // 用户1：2 笔已支付 → 复购
        insertOrder(1L, 1, 1, new BigDecimal("10.00"), LocalDateTime.now());
        insertOrder(1L, 1, 1, new BigDecimal("10.00"), LocalDateTime.now());
        // 用户2：1 笔已支付 → 非复购
        insertOrder(2L, 1, 1, new BigDecimal("10.00"), LocalDateTime.now());
        // 用户3：仅未支付 → 不计入分母
        insertOrder(3L, 0, 0, new BigDecimal("10.00"), null);

        RepurchaseVO vo = adminStatsService.getRepurchaseRate();

        assertEquals(2L, vo.getTotalBuyers(), "有支付记录的用户数为 2");
        assertEquals(1L, vo.getRepeatBuyers(), "复购用户数为 1");
        assertEquals(0, new BigDecimal("50.00").compareTo(vo.getRate()), "复购率应为 50%");
    }

    @Test
    @DisplayName("验收6：无任何支付记录时复购率返回 0 且不报错")
    void repurchaseRateWithoutBuyers() {
        insertOrder(1L, 0, 0, new BigDecimal("10.00"), null);

        RepurchaseVO vo = adminStatsService.getRepurchaseRate();

        assertEquals(0L, vo.getTotalBuyers());
        assertEquals(0L, vo.getRepeatBuyers());
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getRate()));
    }
}
