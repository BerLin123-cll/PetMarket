package com.pet.petmarket.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户复购率 VO
 *
 * <p>后台数据概览「用户复购率」用：在有支付记录的用户中，下单 ≥2 笔的占比。</p>
 */
@Data
public class RepurchaseVO {

    /** 有过已支付订单的用户数（去重） */
    private Long totalBuyers;

    /** 其中下单 ≥2 笔的用户数 */
    private Long repeatBuyers;

    /** 复购率（百分比，保留两位小数，如 33.33 表示 33.33%） */
    private BigDecimal rate;
}
