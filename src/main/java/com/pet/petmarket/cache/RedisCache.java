package com.pet.petmarket.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.petmarket.config.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 缓存操作封装（Cache-Aside 模式）
 *
 * <p>设计目标：<b>缓存出任何问题都不能影响主流程</b>。</p>
 * <ol>
 *   <li><b>故障降级</b>：所有 Redis 调用都包在 try/catch 里，异常只记日志，
 *       读返回 null、写静默跳过 —— 调用方拿不到缓存就回落到数据库，接口照常可用；</li>
 *   <li><b>熔断保护</b>：Redis 不可用时（连接拒绝 / 超时）在 {@link #UNAVAILABLE_BACKOFF_MILLIS}
 *       内直接跳过缓存，避免每个请求都白等一次连接超时，把 Redis 故障放大成接口变慢；</li>
 *   <li><b>JSON 序列化</b>：用项目已有的 {@link ObjectMapper} 写入 UTF-8 JSON，
 *       可读性好、跨语言可查、调整字段不会因 JDK 序列化版本不一致反序列化失败；</li>
 *   <li><b>前缀失效</b>：{@link #evictByPrefix} 使用 SCAN 游标而不是 KEYS，
 *       避免在大 key 空间上阻塞 Redis 单线程。</li>
 * </ol>
 *
 * <p>键的统一规则见 {@link CacheKeys}，过期时间见 {@link CacheProperties}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCache {

    /** Redis 故障后的静默窗口：窗口内所有缓存操作直接短路，30 秒后自动重试 */
    private static final long UNAVAILABLE_BACKOFF_MILLIS = 30_000L;

    /** SCAN 每批建议数量（仅提示，Redis 不保证精确返回这么多） */
    private static final int SCAN_BATCH_SIZE = 200;

    /** 单次前缀失效最多删除的键数，防止误用大前缀时长时间占用连接 */
    private static final int MAX_PREFIX_DELETE_KEYS = 5_000;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    /** 熔断截止时间戳（毫秒），早于当前时间表示可继续尝试 Redis */
    private final AtomicLong unavailableUntil = new AtomicLong(0L);

    /**
     * 缓存是否可尝试访问（开关打开且不在熔断窗口内）
     */
    public boolean isAvailable() {
        return cacheProperties.isEnabled() && System.currentTimeMillis() >= unavailableUntil.get();
    }

    /**
     * 读取缓存（按具体类型反序列化）
     *
     * @param key  缓存键
     * @param type 目标类型，如 {@code ProductVO.class}
     * @return 命中的对象；未命中、缓存关闭或 Redis 异常时返回 null（调用方回落到数据库）
     */
    public <T> T get(String key, Class<T> type) {
        return read(key, objectMapper.getTypeFactory().constructType(type));
    }

    /**
     * 读取缓存（支持泛型，如 {@code List<Category>}）
     *
     * @param key  缓存键
     * @param type 目标类型，如 {@code new TypeReference<List<Category>>() {}}
     * @return 命中的对象；未命中、缓存关闭或 Redis 异常时返回 null（调用方回落到数据库）
     */
    public <T> T get(String key, TypeReference<T> type) {
        return read(key, objectMapper.getTypeFactory().constructType(type));
    }

    /**
     * 写入缓存（带过期时间）
     *
     * <p>value 为 null 时不写入（缓存空值会掩盖后续新增数据）；ttl 非正数时也不写入。</p>
     */
    public void put(String key, Object value, Duration ttl) {
        if (value == null || ttl == null || ttl.isZero() || ttl.isNegative() || !isAvailable()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // 序列化问题是代码/数据结构问题，不是 Redis 故障，不应触发熔断
            log.warn("缓存值序列化失败，跳过写入：key={}，原因={}", key, e.getMessage());
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            markUnavailable("写入", key, e);
        }
    }

    /**
     * 精确删除若干缓存键（写操作后主动失效）
     */
    public void evict(String... keys) {
        if (keys == null || keys.length == 0 || !isAvailable()) {
            return;
        }
        try {
            redisTemplate.delete(Arrays.asList(keys));
        } catch (Exception e) {
            markUnavailable("删除", String.join(",", keys), e);
        }
    }

    /**
     * 按前缀批量失效（SCAN 游标遍历，不阻塞 Redis）
     *
     * <p>用于「从库表反推不出受影响的键」的场景，例如商品改动会影响所有宠物的推荐结果。</p>
     *
     * @param prefix 键前缀，如 {@link CacheKeys#PET_RECOMMEND_PREFIX}
     */
    public void evictByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || !isAvailable()) {
            return;
        }

        ScanOptions options = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(SCAN_BATCH_SIZE)
                .build();

        int deleted = 0;
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> batch = new ArrayList<>(SCAN_BATCH_SIZE);
            while (cursor.hasNext() && deleted < MAX_PREFIX_DELETE_KEYS) {
                batch.add(cursor.next());
                if (batch.size() >= SCAN_BATCH_SIZE) {
                    deleted += deleteBatch(batch);
                }
            }
            deleted += deleteBatch(batch);
        } catch (Exception e) {
            markUnavailable("按前缀删除", prefix, e);
            return;
        }

        if (deleted > 0) {
            log.debug("缓存失效：前缀 {} 共删除 {} 个键", prefix, deleted);
        }
        if (deleted >= MAX_PREFIX_DELETE_KEYS) {
            log.warn("缓存失效：前缀 {} 命中键数达到上限 {}，其余键将在 TTL 到期后自然失效",
                    prefix, MAX_PREFIX_DELETE_KEYS);
        }
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private <T> T read(String key, JavaType javaType) {
        if (!isAvailable()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            // 旧结构 / 被手工改坏的缓存值：删掉这一个键，下次读取自然回填，不影响整体缓存可用性
            log.warn("缓存内容无法反序列化，已删除该键并回落数据库：key={}，原因={}", key, e.getOriginalMessage());
            evict(key);
            return null;
        } catch (Exception e) {
            markUnavailable("读取", key, e);
            return null;
        }
    }

    /**
     * 删除一批键并返回实际删除数量（调用方负责清空 list）
     */
    private int deleteBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            Long removed = redisTemplate.delete(batch);
            return removed == null ? 0 : removed.intValue();
        } finally {
            batch.clear();
        }
    }

    /**
     * 记录一次 Redis 故障并开启（或延长）熔断窗口
     *
     * <p>同一个窗口内只打印一条告警，防止 Redis 长时间不可用时日志被刷爆。</p>
     */
    private void markUnavailable(String action, String key, Exception e) {
        long now = System.currentTimeMillis();
        long previous = unavailableUntil.getAndUpdate(
                prev -> Math.max(prev, now + UNAVAILABLE_BACKOFF_MILLIS));
        if (now >= previous) {
            log.warn("Redis 暂不可用（{} {}）：{}；接下来 {} 秒内自动跳过缓存、直接访问数据库",
                    action, key, e.getMessage(), UNAVAILABLE_BACKOFF_MILLIS / 1000);
        }
    }
}
