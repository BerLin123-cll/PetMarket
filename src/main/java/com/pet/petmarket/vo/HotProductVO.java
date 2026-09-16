package com.pet.petmarket.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销商品 VO
 *
 * <p>后台数据概览「热销商品」用：从已支付订单的订单项中按销量聚合，取 Top N。</p>
 */
@Data
public class HotProductVO {

    private Long productId;

    private String productName;

    /** 累计售出数量 */
    private Long quantity;

    /** 累计销售额（单价 × 数量之和） */
    private BigDecimal amount;
}
