package com.pet.petmarket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收货地址实体类
 */
@Data
@TableName("address")
public class Address {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("receiver_name")
    private String receiverName;
    
    private String phone;
    
    @TableField("detail_address")
    private String detailAddress;
    
    @TableField("is_default")
    private Integer isDefault;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
