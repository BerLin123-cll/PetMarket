package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项数据访问层
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
