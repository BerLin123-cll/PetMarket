package com.pet.petmarket.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pet.petmarket.config.CacheProperties;
import com.pet.petmarket.entity.Category;
import com.pet.petmarket.vo.PetVO;
import com.pet.petmarket.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisCache} 单元测试（不依赖真实 Redis）
 *
 * <p>覆盖点：</p>
 * <ol>
 *   <li>命中 / 未命中缓存；</li>
 *   <li>JSON 序列化（含 {@link LocalDate} 等 Java 8 时间类型）可正确回读；</li>
 *   <li>Redis 异常时读返回 null、写/删不抛异常（主流程降级）；</li>
 *   <li>故障后开启熔断窗口，短时间内不再访问 Redis；</li>
 *   <li>{@code petmarket.cache.enabled=false} 时完全不访问 Redis。</li>
 * </ol>
 */
class RedisCacheTest {

    private static final String KEY = "petmarket:v1:test:1";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CacheProperties cacheProperties;
    private RedisCache redisCache;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        cacheProperties = new CacheProperties();

        // 与生产保持一致：JSON + JavaTimeModule + 时间写字符串（不是数组）
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        redisCache = new RedisCache(redisTemplate, objectMapper, cacheProperties);
    }

    @Test
    @DisplayName("缓存命中：JSON 能反序列化为目标对象")
    void getReturnsDeserializedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("""
                {"id":1,"name":"测试狗粮","categoryId":2,"categoryName":"狗狗用品",
                 "price":99.50,"stock":10,"status":1}
                """);

        ProductVO vo = redisCache.get(KEY, ProductVO.class);

        assertEquals(1L, vo.getId());
        assertEquals("测试狗粮", vo.getName());
        assertEquals(new BigDecimal("99.50"), vo.getPrice());
    }

    @Test
    @DisplayName("缓存未命中：返回 null，由调用方回落数据库")
    void getReturnsNullWhenKeyMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertNull(redisCache.get(KEY, ProductVO.class));
    }

    @Test
    @DisplayName("写入缓存：值序列化为 JSON，并带上 TTL")
    void putWritesJsonWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ProductVO vo = new ProductVO();
        vo.setId(1L);
        vo.setName("测试狗粮");
        redisCache.put(KEY, vo, Duration.ofMinutes(10));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), json.capture(), eq(Duration.ofMinutes(10)));
        assertTrue(json.getValue().contains("\"name\":\"测试狗粮\""), "应写入可读的 UTF-8 JSON：" + json.getValue());
    }

    @Test
    @DisplayName("泛型缓存：List<Category> 与含 LocalDate 的 PetVO 都能正确回读")
    void supportsGenericTypesAndJavaTime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<Category> categories = List.of(category(1L, "狗狗用品"), category(2L, "猫咪用品"));
        redisCache.put(KEY, categories, Duration.ofHours(1));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(KEY), json.capture(), eq(Duration.ofHours(1)));
        when(valueOperations.get(KEY)).thenReturn(json.getValue());

        List<Category> read = redisCache.get(KEY, new TypeReference<List<Category>>() {
        });
        assertEquals(2, read.size());
        assertEquals("猫咪用品", read.get(1).getName());

        PetVO pet = new PetVO();
        pet.setName("咪咪");
        pet.setBirthday(LocalDate.of(2023, 5, 1));
        redisCache.put(KEY, pet, Duration.ofMinutes(5));
        verify(valueOperations, times(2)).set(eq(KEY), json.capture(), any(Duration.class));
        when(valueOperations.get(KEY)).thenReturn(json.getValue());

        PetVO readPet = redisCache.get(KEY, PetVO.class);
        assertEquals(LocalDate.of(2023, 5, 1), readPet.getBirthday(), "LocalDate 必须能随缓存正确回读");
    }

    @Test
    @DisplayName("缓存值损坏：删除脏键并回落数据库，但不触发熔断")
    void corruptedValueIsEvictedWithoutBreakingCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("{not-a-valid-json");
        when(redisTemplate.delete(anyCollection())).thenReturn(1L);

        assertNull(redisCache.get(KEY, ProductVO.class));
        verify(redisTemplate).delete(anyCollection());
        assertTrue(redisCache.isAvailable(), "反序列化失败属于数据问题，不应触发熔断");
    }

    @Test
    @DisplayName("Redis 故障：读返回 null、写与删不抛异常")
    void redisFailureIsSwallowed() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("连接被拒绝"));

        assertNull(assertDoesNotThrow(() -> redisCache.get(KEY, ProductVO.class)));

        // 熔断窗口内：写与删直接短路，不会再次抛异常
        when(redisTemplate.delete(anyCollection())).thenThrow(new RedisConnectionFailureException("连接被拒绝"));
        assertDoesNotThrow(() -> redisCache.put(KEY, new ProductVO(), Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> redisCache.evict(KEY));
        assertDoesNotThrow(() -> redisCache.evictByPrefix(CacheKeys.PRODUCT_DETAIL_PREFIX));
    }

    @Test
    @DisplayName("故障熔断：窗口内不再访问 Redis，避免每个请求都等连接超时")
    void circuitBreakerSkipsRedisAfterFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("连接被拒绝"));

        assertNull(redisCache.get(KEY, ProductVO.class));
        assertFalse(redisCache.isAvailable(), "故障后应进入熔断窗口");

        assertNull(redisCache.get(KEY, ProductVO.class));
        verify(valueOperations, times(1)).get(anyString());
    }

    @Test
    @DisplayName("开关关闭：完全不访问 Redis，接口等价直连数据库")
    void disabledCacheNeverTouchesRedis() {
        cacheProperties.setEnabled(false);

        assertNull(redisCache.get(KEY, ProductVO.class));
        redisCache.put(KEY, new ProductVO(), Duration.ofMinutes(1));
        redisCache.evict(KEY);
        redisCache.evictByPrefix(CacheKeys.PET_RECOMMEND_PREFIX);

        verify(redisTemplate, never()).opsForValue();
        verify(redisTemplate, never()).delete(anyCollection());
        assertFalse(redisCache.isAvailable());
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setStatus(1);
        return category;
    }
}
