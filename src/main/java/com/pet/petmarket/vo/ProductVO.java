package com.pet.petmarket.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品展示 VO
 */
@Data
public class ProductVO {

    private Long id;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String mainImage;

    private BigDecimal price;

    private Integer stock;

    private String description;

    private Integer status;
}
