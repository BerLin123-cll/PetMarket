package com.pet.petmarket.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 购物车展示 VO（包含商品信息）
 */
@Data
public class CartVO {
    
    private Long id;
    
    private Long userId;
    
    private Long productId;
    
    private String productName;
    
    private String productImage;
    
    private BigDecimal price;
    
    private Integer quantity;
    
    private BigDecimal subtotal;
}
