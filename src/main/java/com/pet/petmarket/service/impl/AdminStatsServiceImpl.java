package com.pet.petmarket.service.impl;

import com.pet.petmarket.mapper.OrdersMapper;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.service.AdminStatsService;
import com.pet.petmarket.vo.HotProductVO;
import com.pet.petmarket.vo.LowStockProductVO;
import com.pet.petmarket.vo.OrderStatusCountVO;
import com.pet.petmarket.vo.RepurchaseVO;
import com.pet.petmarket.vo.SalesTrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台数据概览统计服务实现
 *
 * <p>负责把 Mapper 的聚合结果整理成前端可直接渲染的结构：</p>
 * <ul>
 *   <li>销售趋势：按日期连续补 0，避免前端横轴断裂；</li>
 *   <li>订单状态漏斗：补齐缺失状态并写入中文状态名；</li>
 *   <li>复购率：在服务层做百分比换算，避免前端除零。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final OrdersMapper ordersMapper;
    private final ProductMapper productMapper;

    /** 销售趋势默认/最大天数 */
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 30;
    /** 热销商品默认/最大条数 */
    private static final int DEFAULT_HOT_LIMIT = 5;
    private static final int MAX_HOT_LIMIT = 50;
    /** 低库存默认阈值与条数 */
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;
    private static final int DEFAULT_LOW_STOCK_LIMIT = 20;
    private static final int MAX_LOW_STOCK_LIMIT = 100;

    /** 订单状态顺序与展示名 */
    private static final List<Integer> STATUS_ORDER = List.of(0, 1, 2, 3, 4);
    private static final Map<Integer, String> STATUS_TEXT = Map.of(
            0, "待支付",
            1, "已支付",
            2, "已发货",
            3, "已完成",
            4, "已取消");

    @Override
    public List<SalesTrendVO> getSalesTrend(Integer days) {
        int span = normalize(days, DEFAULT_TREND_DAYS, MAX_TREND_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(span - 1L);

        Map<LocalDate, BigDecimal> amountByDate = ordersMapper.selectSalesTrend(start.atStartOfDay())
                .stream()
                .filter(point -> point.getSaleDate() != null)
                .collect(Collectors.toMap(SalesTrendVO::getSaleDate,
                        point -> point.getAmount() == null ? BigDecimal.ZERO : point.getAmount(),
                        BigDecimal::add));

        List<SalesTrendVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate date = start.plusDays(i);
            SalesTrendVO vo = new SalesTrendVO();
            vo.setSaleDate(date);
            vo.setAmount(amountByDate.getOrDefault(date, BigDecimal.ZERO));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<OrderStatusCountVO> getOrderStatusFunnel() {
        Map<Integer, Long> countByStatus = ordersMapper.countGroupByStatus()
                .stream()
                .filter(item -> item.getStatus() != null)
                .collect(Collectors.toMap(OrderStatusCountVO::getStatus,
                        item -> item.getCount() == null ? 0L : item.getCount(),
                        Long::sum));

        List<OrderStatusCountVO> result = new ArrayList<>(STATUS_ORDER.size());
        for (Integer status : STATUS_ORDER) {
            OrderStatusCountVO vo = new OrderStatusCountVO();
            vo.setStatus(status);
            vo.setStatusText(STATUS_TEXT.getOrDefault(status, "未知"));
            vo.setCount(countByStatus.getOrDefault(status, 0L));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<HotProductVO> getHotProducts(Integer limit) {
        return ordersMapper.selectHotProducts(normalize(limit, DEFAULT_HOT_LIMIT, MAX_HOT_LIMIT));
    }

    @Override
    public List<LowStockProductVO> getLowStockProducts(Integer threshold, Integer limit) {
        int stockThreshold = (threshold == null || threshold < 0) ? DEFAULT_LOW_STOCK_THRESHOLD : threshold;
        int top = normalize(limit, DEFAULT_LOW_STOCK_LIMIT, MAX_LOW_STOCK_LIMIT);
        return productMapper.selectLowStock(stockThreshold, top);
    }

    @Override
    public RepurchaseVO getRepurchaseRate() {
        long totalBuyers = nullSafe(ordersMapper.countPaidBuyers());
        long repeatBuyers = nullSafe(ordersMapper.countRepeatBuyers());

        RepurchaseVO vo = new RepurchaseVO();
        vo.setTotalBuyers(totalBuyers);
        vo.setRepeatBuyers(repeatBuyers);
        vo.setRate(totalBuyers == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(repeatBuyers)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalBuyers), 2, RoundingMode.HALF_UP));
        return vo;
    }

    /** 参数为空或非正数时取默认值，并做上限保护 */
    private int normalize(Integer value, int defaultValue, int max) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, max);
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
