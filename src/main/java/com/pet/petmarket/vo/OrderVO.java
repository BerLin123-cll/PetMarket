package com.pet.petmarket.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单展示 VO（包含订单项）
 */
@Data
public class OrderVO {
    
    private Long id;
    
    private String orderNo;
    
    private Long userId;
    
    private BigDecimal totalAmount;
    
    private String receiverName;
    
    private String receiverPhone;
    
    private String receiverAddress;
    
    private Integer orderStatus;
    
    private Integer payStatus;
    
    private LocalDateTime createTime;
    
    private LocalDateTime payTime;
    
    /**
     * 支付截止时间（待支付订单超时后自动取消并回补库存），前端可用于展示支付倒计时
     */
    private LocalDateTime payExpireTime;
    
    private List<OrderItemVO> items;
}
