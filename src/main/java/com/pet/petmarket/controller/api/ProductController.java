package com.pet.petmarket.controller.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.dto.ProductQueryDTO;
import com.pet.petmarket.vo.ProductVO;
import com.pet.petmarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品相关接口（前台）
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    /**
     * 分页查询商品列表
     */
    @GetMapping("/list")
    public Result<IPage<ProductVO>> getProductList(ProductQueryDTO queryDTO) {
        IPage<ProductVO> page = productService.getProductPage(queryDTO);
        return Result.success(page);
    }
    
    /**
     * 获取商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO product = productService.getProductDetail(id);
        return Result.success(product);
    }
    
    /**
     * 首页热门商品推荐
     */
    @GetMapping("/hot")
    public Result<IPage<ProductVO>> getHotProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "8") Integer pageSize) {
        IPage<ProductVO> page = productService.getHotProducts(pageNum, pageSize);
        return Result.success(page);
    }
}
