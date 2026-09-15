package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车数据访问层
 */
@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
