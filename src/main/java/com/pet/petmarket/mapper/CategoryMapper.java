package com.pet.petmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.petmarket.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类数据访问层
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
