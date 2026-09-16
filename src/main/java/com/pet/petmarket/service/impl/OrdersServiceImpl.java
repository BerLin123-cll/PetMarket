package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.config.OrderProperties;
import com.pet.petmarket.dto.CreateOrderDTO;
import com.pet.petmarket.entity.*;
import com.pet.petmarket.mapper.OrdersMapper;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.service.*;
import com.pet.petmarket.vo.OrderItemVO;
import com.pet.petmarket.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务实现类
 *
 * <p>库存一致性设计（P0-3）：</p>
 * <ol>
 *   <li><b>下单预占库存</b>：创建订单时用 {@code UPDATE product SET stock = stock - n WHERE id = ? AND stock >= n}
 *       做条件扣减，把「判断库存」和「扣减库存」合并成一条原子 SQL，由数据库行锁保证并发安全，彻底避免超卖；</li>
 *   <li><b>扣减失败即回滚</b>：任何一个商品扣减失败（返回 0 行）就抛异常，整个事务回滚，
 *       已扣减的商品库存一并恢复，不会出现「扣了 A 没扣 B」的中间态；</li>
 *   <li><b>取消/超时回补</b>：用户取消、超时自动取消都走「条件状态流转 + 回补库存」，
 *       状态流转用 CAS 保证同一订单只会回补一次，避免并发下重复加库存；</li>
 *   <li><b>超时自动取消</b>：订单带 {@code pay_expire_time}，由 {@code OrderTimeoutTask} 定时扫描释放。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    /** 订单状态：待支付 */
    private static final int STATUS_UNPAID = 0;
    /** 订单状态：已支付 */
    private static final int STATUS_PAID = 1;
    /** 订单状态：已发货 */
    private static final int STATUS_SHIPPED = 2;
    /** 订单状态：已取消 */
    private static final int STATUS_CANCELLED = 4;

    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;
    private final OrderItemService orderItemService;
    private final OrderProperties orderProperties;
    private final ProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, CreateOrderDTO createOrderDTO) {
        Address address = addressService.getById(createOrderDTO.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        List<Cart> carts = cartService.listByIds(createOrderDTO.getCartIds());
        if (carts.isEmpty()) {
            throw new BusinessException(ResultCode.CART_NOT_FOUND);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Cart cart : carts) {
            if (!cart.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该购物车项");
            }

            Product product = productService.getById(cart.getProductId());
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }

            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(ResultCode.PRODUCT_OFF_SHELF,
                        "商品【" + product.getName() + "】已下架，请从购物车移除后重试");
            }

            // 关键：条件扣减（原子操作），返回 0 表示库存不足，直接抛异常触发整单回滚
            int affected = productMapper.deductStock(product.getId(), cart.getQuantity());
            if (affected == 0) {
                throw new BusinessException(ResultCode.PRODUCT_OUT_OF_STOCK,
                        "商品【" + product.getName() + "】库存不足");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItems.add(orderItem);
        }

        LocalDateTime now = LocalDateTime.now();

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getDetailAddress());
        order.setOrderStatus(STATUS_UNPAID);
        order.setPayStatus(0);
        // 库存预占后的支付截止时间：到期未支付由定时任务自动取消并回补库存
        order.setPayExpireTime(now.plusMinutes(orderProperties.getPayTimeoutMinutes()));

        this.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
        }
        orderItemService.saveBatch(orderItems);

        cartService.removeByIds(createOrderDTO.getCartIds());

        return convertToVO(order, orderItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long userId, String orderNo) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getOrderNo, orderNo)
               .eq(Orders::getUserId, userId);

        Orders order = this.getOne(wrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        if (order.getOrderStatus() != STATUS_UNPAID) {
            if (order.getOrderStatus() == STATUS_CANCELLED) {
                throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单已取消（可能已超时未支付），无法支付");
            }
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 支付超时判断：即使定时任务尚未执行，也不允许支付已过期订单
        if (order.getPayExpireTime() != null && order.getPayExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单已超过支付时限，请重新下单");
        }

        // 条件更新：只有「仍为待支付且未支付」时才更新成功，防止重复支付
        int affected = baseMapper.markPaidIfUnpaid(order.getId(), STATUS_UNPAID, STATUS_PAID, LocalDateTime.now());
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，支付失败，请刷新后重试");
        }
    }

    @Override
    public IPage<OrderVO> getUserOrders(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        Page<Orders> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);

        if (status != null) {
            wrapper.eq(Orders::getOrderStatus, status);
        }

        wrapper.orderByDesc(Orders::getCreateTime);

        IPage<Orders> orderPage = this.page(page, wrapper);

        IPage<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemService.getByOrderId(order.getId());
                    return convertToVO(order, items);
                })
                .toList());

        return voPage;
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        List<OrderItem> items = orderItemService.getByOrderId(orderId);
        return convertToVO(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        if (order.getOrderStatus() != STATUS_UNPAID) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只能取消待支付的订单");
        }

        if (!releaseStock(order)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，取消失败，请刷新后重试");
        }
    }

    /**
     * 释放订单占用的库存（内部实现）
     *
     * <p>先用 CAS 条件更新把订单从「待支付」流转为「已取消」：</p>
     * <ul>
     *   <li>更新成功（返回 1）：说明本次调用抢到了释放权，继续回补库存；</li>
     *   <li>更新失败（返回 0）：说明订单已被其他线程（用户取消 / 定时任务）处理过，
     *       直接返回 false，绝不重复回补库存。</li>
     * </ul>
     *
     * @return true=本次调用完成取消与回补；false=订单已被处理
     */
    private boolean releaseStock(Orders order) {
        int affected = baseMapper.updateStatusIfMatch(order.getId(), STATUS_UNPAID, STATUS_CANCELLED);
        if (affected == 0) {
            log.info("订单[{}]状态已被其他线程流转，跳过库存回补", order.getOrderNo());
            return false;
        }

        List<OrderItem> items = orderItemService.getByOrderId(order.getId());
        for (OrderItem item : items) {
            int restored = productMapper.restoreStock(item.getProductId(), item.getQuantity());
            if (restored == 0) {
                log.warn("订单[{}]回补库存失败，商品[{}]可能已被删除", order.getOrderNo(), item.getProductId());
            }
        }
        log.info("订单[{}]已取消，回补 {} 个商品的库存", order.getOrderNo(), items.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseTimeoutOrder(Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null) {
            return false;
        }
        // 只处理仍未支付的订单；支付状态/状态流转由 releaseStock 内部 CAS 保证幂等
        if (order.getOrderStatus() != STATUS_UNPAID || order.getPayStatus() != 0) {
            return false;
        }
        return releaseStock(order);
    }

    @Override
    public int releaseTimeoutOrders(int batchSize) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderProperties.getPayTimeoutMinutes());
        List<Long> timeoutOrderIds = baseMapper.selectTimeoutOrderIds(deadline, batchSize);
        if (timeoutOrderIds.isEmpty()) {
            return 0;
        }

        int released = 0;
        for (Long orderId : timeoutOrderIds) {
            try {
                if (releaseTimeoutOrder(orderId)) {
                    released++;
                }
            } catch (Exception e) {
                // 单个订单失败不影响其他订单
                log.error("超时订单[{}]自动取消失败：{}", orderId, e.getMessage(), e);
            }
        }
        if (released > 0) {
            log.info("超时订单自动取消完成：扫描 {} 单，成功释放 {} 单", timeoutOrderIds.size(), released);
        }
        return released;
    }

    @Override
    public IPage<OrderVO> getAllOrders(Integer pageNum, Integer pageSize, Integer status) {
        Page<Orders> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Orders::getOrderStatus, status);
        }
        wrapper.orderByDesc(Orders::getCreateTime);

        IPage<Orders> orderPage = this.page(page, wrapper);

        IPage<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemService.getByOrderId(order.getId());
                    return convertToVO(order, items);
                })
                .toList());

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        if (order.getOrderStatus() != STATUS_PAID) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只能对已支付的订单进行发货");
        }

        int affected = baseMapper.updateStatusIfMatch(orderId, STATUS_PAID, STATUS_SHIPPED);
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态已变更，发货失败，请刷新后重试");
        }
    }

    @Override
    public BigDecimal getTotalSales() {
        BigDecimal total = baseMapper.sumPaidAmount();
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 8).toUpperCase();
    }

    /**
     * 转换为 VO 对象
     */
    private OrderVO convertToVO(Orders order, List<OrderItem> items) {
        OrderVO vo = BeanUtil.copyProperties(order, OrderVO.class);

        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = BeanUtil.copyProperties(item, OrderItemVO.class);
            itemVO.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemVO;
        }).toList();

        vo.setItems(itemVOs);

        return vo;
    }
}
