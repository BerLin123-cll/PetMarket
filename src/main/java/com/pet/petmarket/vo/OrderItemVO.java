package com.pet.petmarket.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项展示 VO
 */
@Data
public class OrderItemVO {
    
    private Long id;
    
    private Long productId;
    
    private String productName;
    
    private String productImage;
    
    private BigDecimal price;
    
    private Integer quantity;
    
    private BigDecimal subtotal;
}
