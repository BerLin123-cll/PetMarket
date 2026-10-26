package com.pet.petmarket.cache;

import com.pet.petmarket.entity.Category;
import com.pet.petmarket.entity.Pet;
import com.pet.petmarket.entity.Product;
import com.pet.petmarket.mapper.CategoryMapper;
import com.pet.petmarket.mapper.ProductMapper;
import com.pet.petmarket.service.CategoryService;
import com.pet.petmarket.service.PetService;
import com.pet.petmarket.service.ProductService;
import com.pet.petmarket.vo.ProductVO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 缓存集成测试（真实连接本地 Redis）
 *
 * <p>验证的是「服务层缓存行为」而不只是 Redis 连通性：</p>
 * <ol>
 *   <li>第一次读取回填缓存，第二次读取直接命中缓存（绕过 service 直接改库也读不到新值）；</li>
 *   <li>走 service 的写操作会主动失效缓存，之后读到的是最新数据；</li>
 *   <li>商品变更会按前缀失效宠物推荐缓存。</li>
 * </ol>
 *
 * <p><b>前置条件</b>：本地 Redis 已启动（项目根目录执行 {@code docker compose up -d}）。
 * 未启动时本测试自动跳过，不会让 CI / 无 Redis 环境失败。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_cache;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "petmarket.cache.enabled=true"
})
class RedisCacheIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private PetService petService;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(redisReachable(),
                "本地 Redis 未启动，跳过缓存集成测试（先执行 docker compose up -d）");
        clearCacheKeys();
    }

    @Test
    @DisplayName("分类列表：首次读回填缓存，写操作主动失效")
    void categoryListIsCachedAndInvalidatedOnWrite() {
        Long categoryId = createCategory("缓存测试分类");

        // 1. 第一次读：缓存未命中 → 查库 → 回填
        assertEquals("缓存测试分类", findCategoryName(categoryService.getActiveCategories(), categoryId));

        // 2. 绕过 service 直接改库：若缓存生效，这里仍应读到旧名字
        Category direct = new Category();
        direct.setId(categoryId);
        direct.setName("直接改库的名字");
        categoryMapper.updateById(direct);
        assertEquals("缓存测试分类", findCategoryName(categoryService.getActiveCategories(), categoryId),
                "第二次读取应命中缓存，而不是直接读库");

        // 3. 走 service 的写操作：应主动失效缓存
        Category update = new Category();
        update.setId(categoryId);
        update.setName("走服务改的名字");
        categoryService.updateCategory(update);
        assertEquals("走服务改的名字", findCategoryName(categoryService.getActiveCategories(), categoryId),
                "分类写操作后缓存应被失效");
    }

    @Test
    @DisplayName("商品详情：首次读回填缓存，更新后缓存失效")
    void productDetailIsCachedAndInvalidatedOnUpdate() {
        Long productId = createProduct("缓存测试狗粮", 1L);

        assertEquals("缓存测试狗粮", productService.getProductDetail(productId).getName());

        Product direct = new Product();
        direct.setId(productId);
        direct.setName("直接改库商品");
        productMapper.updateById(direct);
        assertEquals("缓存测试狗粮", productService.getProductDetail(productId).getName(),
                "第二次读取应命中缓存，而不是直接读库");

        Product update = new Product();
        update.setId(productId);
        update.setName("走服务改的商品");
        productService.updateProduct(update);
        assertEquals("走服务改的商品", productService.getProductDetail(productId).getName(),
                "商品更新后详情缓存应被失效");
    }

    @Test
    @DisplayName("宠物推荐：按宠物维度缓存，商品变更后按前缀失效")
    void petRecommendationIsCachedAndInvalidatedByProductChange() {
        Long productId = createProduct("幼犬推荐狗粮", 1L);

        Pet pet = new Pet();
        pet.setName("缓存测试犬");
        pet.setSpecies(1);
        pet.setBirthday(LocalDate.now().minusMonths(3));
        Long petId = petService.addPet(USER_ID, pet).getId();

        // 1. 首次推荐：查库并回填缓存
        assertTrue(hasProductNamed(petService.recommendProducts(USER_ID, petId), "幼犬推荐狗粮"));

        // 2. 绕过 service 直接改库：缓存命中时仍是旧名字
        Product direct = new Product();
        direct.setId(productId);
        direct.setName("直接改库推荐商品");
        productMapper.updateById(direct);
        assertTrue(hasProductNamed(petService.recommendProducts(USER_ID, petId), "幼犬推荐狗粮"),
                "第二次推荐应命中缓存，而不是直接读库");

        // 3. 商品写操作：推荐缓存按前缀整体失效，读到新名字
        Product update = new Product();
        update.setId(productId);
        update.setName("走服务改的推荐商品");
        productService.updateProduct(update);
        assertTrue(hasProductNamed(petService.recommendProducts(USER_ID, petId), "走服务改的推荐商品"),
                "商品变更后推荐缓存应被失效");
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    private boolean redisReachable() {
        try {
            String pong = redisTemplate.execute((RedisCallback<String>) RedisConnection::ping);
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }

    /** 清掉本项目业务缓存键，避免上一次运行的残留影响断言 */
    private void clearCacheKeys() {
        Set<String> keys = redisTemplate.keys(CacheKeys.PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Long createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSortOrder(99);
        category.setStatus(1);
        categoryService.createCategory(category);
        return category.getId();
    }

    private Long createProduct(String name, Long categoryId) {
        Product product = new Product();
        product.setName(name);
        product.setCategoryId(categoryId);
        product.setPrice(new BigDecimal("20.00"));
        product.setStock(10);
        product.setStatus(1);
        productService.createProduct(product);
        return product.getId();
    }

    private static String findCategoryName(List<Category> categories, Long categoryId) {
        return categories.stream()
                .filter(category -> Objects.equals(category.getId(), categoryId))
                .map(Category::getName)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasProductNamed(List<ProductVO> products, String keyword) {
        return products.stream()
                .anyMatch(product -> product.getName() != null && product.getName().contains(keyword));
    }
}
