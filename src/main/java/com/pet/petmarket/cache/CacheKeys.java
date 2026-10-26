package com.pet.petmarket.cache;

/**
 * Redis 缓存键统一管理
 *
 * <p>约定：</p>
 * <ul>
 *   <li>统一前缀 {@code petmarket:v1:}，避免与同一 Redis 实例里其他项目的键冲突；</li>
 *   <li>前缀里的 {@code v1} 是结构版本号：缓存内容结构发生不兼容变化时改版本号，
 *       老键自然过期，不需要手工清理；</li>
 *   <li>以 {@code *} 结尾的常量是「前缀」，用于批量失效（SCAN + 删除），不要直接当键使用。</li>
 * </ul>
 */
public final class CacheKeys {

    /** 所有业务缓存键的统一前缀（含结构版本号） */
    public static final String PREFIX = "petmarket:v1:";

    /** 前台启用的分类列表（无参数，单键） */
    public static final String CATEGORY_ACTIVE_LIST = PREFIX + "category:active-list";

    /** 商品详情键前缀，完整键：petmarket:v1:product:detail:{productId} */
    public static final String PRODUCT_DETAIL_PREFIX = PREFIX + "product:detail:";

    /** 宠物推荐键前缀，完整键：petmarket:v1:pet:recommend:{petId} */
    public static final String PET_RECOMMEND_PREFIX = PREFIX + "pet:recommend:";

    private CacheKeys() {
    }

    /** 商品详情缓存键 */
    public static String productDetail(Long productId) {
        return PRODUCT_DETAIL_PREFIX + productId;
    }

    /** 宠物推荐缓存键（按宠物维度，宠物档案变更时精确失效） */
    public static String petRecommend(Long petId) {
        return PET_RECOMMEND_PREFIX + petId;
    }
}
