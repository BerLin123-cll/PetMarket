package com.pet.petmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("orders")
public class Orders {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("order_no")
    private String orderNo;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("total_amount")
    private BigDecimal totalAmount;
    
    @TableField("receiver_name")
    private String receiverName;
    
    @TableField("receiver_phone")
    private String receiverPhone;
    
    @TableField("receiver_address")
    private String receiverAddress;
    
    @TableField("order_status")
    private Integer orderStatus;
    
    @TableField("pay_status")
    private Integer payStatus;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField("pay_time")
    private LocalDateTime payTime;
    
    /**
     * 支付截止时间：超过该时间仍未支付，订单会被自动取消并回补库存。
     * 该字段比「按 create_time 推算」更直观，也便于前端展示支付倒计时。
     */
    @TableField("pay_expire_time")
    private LocalDateTime payExpireTime;
}
