package com.pet.petmarket.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.service.OrdersService;
import com.pet.petmarket.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器 - 后台管理端
 */
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {
    
    private final OrdersService ordersService;
    
    /**
     * 获取所有订单列表
     */
    @GetMapping("/list")
    public Result<IPage<OrderVO>> getAllOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        
        IPage<OrderVO> page = ordersService.getAllOrders(pageNum, pageSize, status);
        return Result.success(page);
    }
    
    /**
     * 发货
     */
    @PostMapping("/ship/{orderId}")
    public Result<Void> shipOrder(@PathVariable Long orderId) {
        ordersService.shipOrder(orderId);
        return Result.success();
    }
}
