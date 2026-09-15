package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址数据访问层
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
}
