package com.pet.petmarket.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理控制器 - 后台管理端
 */
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * 获取所有分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> getCategoryList() {
        List<Category> categories = categoryService.list(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder)
        );
        return Result.success(categories);
    }
    
    /**
     * 创建分类
     */
    @PostMapping("/create")
    public Result<Void> createCategory(@RequestBody Category category) {
        categoryService.createCategory(category);
        return Result.success();
    }
    
    /**
     * 更新分类
     */
    @PutMapping("/update/{categoryId}")
    public Result<Void> updateCategory(@PathVariable Long categoryId, @RequestBody Category category) {
        category.setId(categoryId);
        categoryService.updateCategory(category);
        return Result.success();
    }
    
    /**
     * 删除分类
     */
    @DeleteMapping("/delete/{categoryId}")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return Result.success();
    }
}
