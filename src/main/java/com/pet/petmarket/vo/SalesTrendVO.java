package com.pet.petmarket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售趋势数据点 VO
 *
 * <p>后台数据概览「销售趋势」用：按天统计已支付订单的销售额，缺失的日期由服务层补 0，
 * 保证前端折线/柱状图横轴连续。</p>
 */
@Data
public class SalesTrendVO {

    /** 统计日期 */
    private LocalDate saleDate;

    /** 当日销售额（已支付订单 total_amount 之和） */
    private BigDecimal amount;
}
