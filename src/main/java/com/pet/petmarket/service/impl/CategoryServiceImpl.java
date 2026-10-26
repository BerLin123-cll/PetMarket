package com.pet.petmarket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.pet.petmarket.cache.CacheKeys;
import com.pet.petmarket.cache.RedisCache;
import com.pet.petmarket.common.BusinessException;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.config.CacheProperties;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.mapper.CategoryMapper;
import com.pet.petmarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现类
 *
 * <p>分类列表是「读多写极少」的典型数据，读接口走 Redis 缓存（Cache-Aside）；
 * 任何分类写操作都会主动失效分类列表，并连带失效冗余了分类名的商品详情与宠物推荐缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    /** 分类列表缓存的泛型类型，避免每次读取都重复构造 */
    private static final TypeReference<List<Category>> CATEGORY_LIST_TYPE = new TypeReference<>() {
    };

    private final RedisCache redisCache;
    private final CacheProperties cacheProperties;

    @Override
    public List<Category> getActiveCategories() {
        List<Category> cached = redisCache.get(CacheKeys.CATEGORY_ACTIVE_LIST, CATEGORY_LIST_TYPE);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .orderByAsc(Category::getSortOrder);
        List<Category> categories = this.list(wrapper);

        redisCache.put(CacheKeys.CATEGORY_ACTIVE_LIST, categories, cacheProperties.getCategoryListTtl());
        return categories;
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
        evictCategoryRelatedCache();
    }

    @Override
    public void updateCategory(Category category) {
        if (category.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类ID不能为空");
        }
        this.updateById(category);
        evictCategoryRelatedCache();
    }

    @Override
    public void deleteCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类ID不能为空");
        }
        this.removeById(categoryId);
        evictCategoryRelatedCache();
    }

    /**
     * 分类变更后的缓存失效
     *
     * <p>分类名被冗余写进了商品详情（categoryName）和宠物推荐结果，所以除了分类列表，
     * 还要按前缀清掉这两类缓存，否则改名后要等 TTL 到期才生效。</p>
     */
    private void evictCategoryRelatedCache() {
        redisCache.evict(CacheKeys.CATEGORY_ACTIVE_LIST);
        redisCache.evictByPrefix(CacheKeys.PRODUCT_DETAIL_PREFIX);
        redisCache.evictByPrefix(CacheKeys.PET_RECOMMEND_PREFIX);
    }
}
