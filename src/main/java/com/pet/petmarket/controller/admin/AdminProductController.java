package com.pet.petmarket.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.dto.ProductQueryDTO;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理控制器 - 后台管理端
 */
@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 分页查询商品列表（包含下架商品）
     */
    @GetMapping("/list")
    public Result<IPage<Product>> getProductList(ProductQueryDTO queryDTO) {
        Page<Product> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.like(Product::getName, queryDTO.getKeyword());
        }

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, queryDTO.getCategoryId());
        }

        wrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> productPage = productService.page(page, wrapper);
        return Result.success(productPage);
    }

    /**
     * 创建商品
     */
    @PostMapping("/create")
    public Result<Void> createProduct(@RequestBody Product product) {
        productService.createProduct(product);
        return Result.success();
    }

    /**
     * 更新商品
     */
    @PutMapping("/update/{productId}")
    public Result<Void> updateProduct(@PathVariable Long productId, @RequestBody Product product) {
        product.setId(productId);
        productService.updateProduct(product);
        return Result.success();
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/delete/{productId}")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return Result.success();
    }

    /**
     * 上下架商品
     */
    @PostMapping("/toggleStatus/{productId}")
    public Result<Void> toggleProductStatus(@PathVariable Long productId, @RequestParam Integer status) {
        productService.toggleProductStatus(productId, status);
        return Result.success();
    }
}
