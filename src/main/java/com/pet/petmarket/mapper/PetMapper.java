package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Pet;
import org.apache.ibatis.annotations.Mapper;

/**
 * 宠物档案数据访问层
 */
@Mapper
public interface PetMapper extends BaseMapper<Pet> {
}
