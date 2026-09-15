package com.pet.petmarket.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pet.petmarket.dto.CreateOrderDTO;
import com.pet.petmarket.entity.Orders;
import com.pet.petmarket.vo.OrderVO;

/**
 * 订单服务接口
 */
public interface OrdersService extends IService<Orders> {

    /**
     * 创建订单
     */
    OrderVO createOrder(Long userId, CreateOrderDTO createOrderDTO);

    /**
     * 模拟支付
     */
    void payOrder(Long userId, String orderNo);

    /**
     * 获取用户订单列表
     */
    IPage<OrderVO> getUserOrders(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 获取订单详情
     */
    OrderVO getOrderDetail(Long userId, Long orderId);

    /**
     * 取消订单（用户主动取消，回补库存）
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 释放单个超时未支付订单：置为已取消并回补库存（幂等）
     *
     * @param orderId 订单ID
     * @return true=本次调用完成取消与回补；false=订单不存在或已被处理
     */
    boolean releaseTimeoutOrder(Long orderId);

    /**
     * 批量扫描并释放超时未支付订单（定时任务调用）
     *
     * @param batchSize 单批最大处理数量
     * @return 成功释放的订单数量
     */
    int releaseTimeoutOrders(int batchSize);

    /**
     * 管理员获取所有订单列表
     */
    IPage<OrderVO> getAllOrders(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员发货
     */
    void shipOrder(Long orderId);
}
