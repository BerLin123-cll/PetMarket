package com.pet.petmarket.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.dto.CreateOrderDTO;
import com.pet.petmarket.service.OrdersService;
import com.pet.petmarket.vo.OrderVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单相关接口（前台）
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrdersService ordersService;
    
    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<OrderVO> createOrder(HttpServletRequest request, @Valid @RequestBody CreateOrderDTO createOrderDTO) {
        Long userId = (Long) request.getAttribute("userId");
        OrderVO order = ordersService.createOrder(userId, createOrderDTO);
        return Result.success(order);
    }
    
    /**
     * 模拟支付
     */
    @PostMapping("/pay")
    public Result<Void> payOrder(HttpServletRequest request, @RequestBody PayRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        ordersService.payOrder(userId, req.getOrderNo());
        return Result.success();
    }
    
    /**
     * 获取用户订单列表
     */
    @GetMapping("/list")
    public Result<IPage<OrderVO>> getUserOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = (Long) request.getAttribute("userId");
        IPage<OrderVO> page = ordersService.getUserOrders(userId, pageNum, pageSize, status);
        return Result.success(page);
    }
    
    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        OrderVO order = ordersService.getOrderDetail(userId, id);
        return Result.success(order);
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    public Result<Void> cancelOrder(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        ordersService.cancelOrder(userId, id);
        return Result.success();
    }
    
    @Data
    static class PayRequest {
        private String orderNo;
    }
}
