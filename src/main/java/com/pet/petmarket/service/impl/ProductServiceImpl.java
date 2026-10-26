package com.pet.petmarket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.petmarket.cache.CacheKeys;
import com.pet.petmarket.cache.RedisCache;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.config.CacheProperties;
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
 *
 * <p>商品详情走 Redis 缓存（Cache-Aside，键 {@code petmarket:v1:product:detail:{id}}）：
 * 详情页是最高频的读接口，而商品信息变更频率低，缓存收益明显。</p>
 *
 * <p><b>缓存失效</b>：新增 / 修改 / 删除 / 上下架都会清掉对应详情键；
 * 由于商品集合变化会影响「宠物个性化推荐」，同时按前缀清掉推荐缓存。
 * 分页列表（查询条件组合多）不做缓存，避免键数量爆炸。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final CategoryService categoryService;
    private final RedisCache redisCache;
    private final CacheProperties cacheProperties;

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
        // 1. 先读缓存；Redis 关闭 / 故障时这里返回 null，自动回落到数据库
        String cacheKey = CacheKeys.productDetail(productId);
        ProductVO cached = redisCache.get(cacheKey, ProductVO.class);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中：查库 + 组装 VO + 回填缓存
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        ProductVO vo = convertToVO(product);
        redisCache.put(cacheKey, vo, cacheProperties.getProductDetailTtl());
        return vo;
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
        // 新商品可能进入宠物推荐候选池
        evictRecommendCache();
    }

    @Override
    public void updateProduct(Product product) {
        if (product.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        this.updateById(product);
        evictProductCache(product.getId());
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品ID不能为空");
        }
        this.removeById(productId);
        evictProductCache(productId);
    }

    @Override
    public void toggleProductStatus(Long productId, Integer status) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        product.setStatus(status);
        this.updateById(product);
        evictProductCache(productId);
    }

    /**
     * 商品变更后的缓存失效：详情键精确删除 + 推荐缓存按前缀删除
     */
    private void evictProductCache(Long productId) {
        redisCache.evict(CacheKeys.productDetail(productId));
        evictRecommendCache();
    }

    /**
     * 商品集合变化会影响「物种 → 分类 → 商品」的推荐结果，推荐缓存只能按前缀整体失效
     */
    private void evictRecommendCache() {
        redisCache.evictByPrefix(CacheKeys.PET_RECOMMEND_PREFIX);
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
