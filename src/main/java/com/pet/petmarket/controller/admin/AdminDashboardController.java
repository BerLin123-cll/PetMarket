package com.pet.petmarket.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.entity.Orders;
import com.pet.petmarket.service.OrdersService;
import com.pet.petmarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据概览控制器 - 后台管理端
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    
    private final UserService userService;
    private final OrdersService ordersService;
    
    /**
     * 获取数据统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户总数
        long userCount = userService.count();
        stats.put("userCount", userCount);
        
        // 订单总数
        long orderCount = ordersService.count();
        stats.put("orderCount", orderCount);
        
        // 销售额统计（已支付的订单总金额）
        BigDecimal totalSales = ordersService.list(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getPayStatus, 1)
        ).stream()
                .map(Orders::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalSales", totalSales);
        
        return Result.success(stats);
    }
}
