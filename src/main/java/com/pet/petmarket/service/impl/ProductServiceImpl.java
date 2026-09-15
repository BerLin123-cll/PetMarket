package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.dto.ProductQueryDTO;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.service.CategoryService;
import com.pet.petmarket.service.ProductService;
import com.pet.petmarket.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商品服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryService categoryService;

    @Override
    public IPage<ProductVO> getProductPage(ProductQueryDTO queryDTO) {
        Page<Product> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);

        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, queryDTO.getCategoryId());
        }

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            wrapper.like(Product::getName, queryDTO.getKeyword());
        }

        if ("price".equals(queryDTO.getSortField())) {
            if ("asc".equalsIgnoreCase(queryDTO.getSortOrder())) {
                wrapper.orderByAsc(Product::getPrice);
            } else {
                wrapper.orderByDesc(Product::getPrice);
            }
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        IPage<Product> productPage = this.page(page, wrapper);

        IPage<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    @Override
    public ProductVO getProductDetail(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException(ResultCode.PRODUCT_NOT_FOUND.getMessage());
        }
        return convertToVO(product);
    }

    @Override
    public IPage<ProductVO> getHotProducts(Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
               .orderByDesc(Product::getCreateTime);

        IPage<Product> productPage = this.page(page, wrapper);

        IPage<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    @Override
    public void createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        this.save(product);
    }

    @Override
    public void updateProduct(Product product) {
        if (product.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        this.updateById(product);
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productId == null) {
            throw new RuntimeException(ResultCode.ERROR.getMessage());
        }
        this.removeById(productId);
    }

    @Override
    public void toggleProductStatus(Long productId, Integer status) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException(ResultCode.PRODUCT_NOT_FOUND.getMessage());
        }
        product.setStatus(status);
        this.updateById(product);
    }

    /**
     * 转换为 VO 对象
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = BeanUtil.copyProperties(product, ProductVO.class);

        Category category = categoryService.getById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        return vo;
    }
}
