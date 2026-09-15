package com.pet.petmarket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 创建订单请求 DTO
 */
@Data
public class CreateOrderDTO {
    
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;
    
    @NotEmpty(message = "购物车项不能为空")
    private List<Long> cartIds;
}
