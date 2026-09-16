package com.pet.petmarket.controller.admin;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.service.AdminStatsService;
import com.pet.petmarket.service.OrdersService;
import com.pet.petmarket.service.UserService;
import com.pet.petmarket.vo.HotProductVO;
import com.pet.petmarket.vo.LowStockProductVO;
import com.pet.petmarket.vo.OrderStatusCountVO;
import com.pet.petmarket.vo.RepurchaseVO;
import com.pet.petmarket.vo.SalesTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
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
    private final AdminStatsService adminStatsService;
    
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
        
        // 销售额统计（已支付订单总金额）：由数据库 SUM 聚合，避免全表加载到内存
        BigDecimal totalSales = ordersService.getTotalSales();
        stats.put("totalSales", totalSales);
        
        return Result.success(stats);
    }

    /**
     * 销售趋势：近 N 天每日销售额（已支付订单），缺失日期补 0
     */
    @GetMapping("/sales-trend")
    public Result<List<SalesTrendVO>> getSalesTrend(@RequestParam(required = false) Integer days) {
        return Result.success(adminStatsService.getSalesTrend(days));
    }

    /**
     * 订单状态漏斗：各状态订单数量（含 0 值状态）
     */
    @GetMapping("/order-status")
    public Result<List<OrderStatusCountVO>> getOrderStatusFunnel() {
        return Result.success(adminStatsService.getOrderStatusFunnel());
    }

    /**
     * 热销商品：已支付订单中销量最高的前 N 个商品
     */
    @GetMapping("/hot-products")
    public Result<List<HotProductVO>> getHotProducts(@RequestParam(required = false) Integer limit) {
        return Result.success(adminStatsService.getHotProducts(limit));
    }

    /**
     * 低库存预警：上架中且库存 ≤ 阈值的商品
     */
    @GetMapping("/low-stock")
    public Result<List<LowStockProductVO>> getLowStockProducts(
            @RequestParam(required = false) Integer threshold,
            @RequestParam(required = false) Integer limit) {
        return Result.success(adminStatsService.getLowStockProducts(threshold, limit));
    }

    /**
     * 用户复购率
     */
    @GetMapping("/repurchase-rate")
    public Result<RepurchaseVO> getRepurchaseRate() {
        return Result.success(adminStatsService.getRepurchaseRate());
    }
}
