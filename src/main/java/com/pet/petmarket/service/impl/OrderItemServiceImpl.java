package com.pet.petmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.entity.OrderItem;
import com.pet.petmarket.mapper.OrderItemMapper;
import com.pet.petmarket.service.OrderItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单项服务实现类
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    @Override
    public List<OrderItem> getByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return this.list(wrapper);
    }
}
