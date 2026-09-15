package com.pet.petmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.mapper.CategoryMapper;
import com.pet.petmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现类
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getActiveCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .orderByAsc(Category::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public void createCategory(Category category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        this.save(category);
    }

    @Override
    public void updateCategory(Category category) {
        if (category.getId() == null) {
            throw new RuntimeException("分类ID不能为空");
        }
        this.updateById(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (categoryId == null) {
            throw new RuntimeException(ResultCode.ERROR.getMessage());
        }
        this.removeById(categoryId);
    }
}
