package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.common.BusinessException;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        voPage.setRecords(convertToVOList(productPage.getRecords()));

        return voPage;
    }

    @Override
    public ProductVO getProductDetail(Long productId) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
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
        voPage.setRecords(convertToVOList(productPage.getRecords()));

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
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        this.updateById(product);
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        this.removeById(productId);
    }

    @Override
    public void toggleProductStatus(Long productId, Integer status) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(status);
        this.updateById(product);
    }

    /**
     * 单个商品转换为 VO（仅用于详情页，附带一次分类查询）
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = BeanUtil.copyProperties(product, ProductVO.class);

        Category category = categoryService.getById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        return vo;
    }

    /**
     * 批量转换为 VO：一次性查出所有商品涉及的分类，避免逐条查分类造成 N+1 查询
     */
    private List<ProductVO> convertToVOList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName));

        return products.stream().map(product -> {
            ProductVO vo = BeanUtil.copyProperties(product, ProductVO.class);
            vo.setCategoryName(categoryNameMap.get(product.getCategoryId()));
            return vo;
        }).toList();
    }
}
