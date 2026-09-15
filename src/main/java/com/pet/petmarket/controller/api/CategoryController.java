package com.pet.petmarket.controller.api;

import com.pet.petmarket.common.Result;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类相关接口（前台）
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    /**
     * 获取所有上架的分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> getCategoryList() {
        List<Category> categories = categoryService.getActiveCategories();
        return Result.success(categories);
    }
}
