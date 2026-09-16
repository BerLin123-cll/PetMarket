package com.pet.petmarket.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 低库存商品 VO
 *
 * <p>后台数据概览「低库存预警」用：筛出上架中且库存低于阈值的商品。</p>
 */
@Data
public class LowStockProductVO {

    private Long id;

    private String name;

    /** 当前库存 */
    private Integer stock;

    private BigDecimal price;
}
