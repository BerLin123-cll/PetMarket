package com.pet.petmarket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存相关配置
 *
 * <p>对应 application.yml 中的 {@code petmarket.cache.*}，支持环境变量覆盖。</p>
 *
 * <p><b>降级开关</b>：{@code enabled=false} 时所有缓存读写直接短路，接口等价于直连数据库，
 * 便于本地没有 Redis 时启动、或线上排查「是不是缓存导致的脏数据」。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "petmarket.cache")
public class CacheProperties {

    /** 是否启用 Redis 缓存（关闭后读写都短路，只走数据库） */
    private boolean enabled = true;

    /** 商品详情缓存有效期 */
    private Duration productDetailTtl = Duration.ofMinutes(10);

    /** 前台分类列表缓存有效期 */
    private Duration categoryListTtl = Duration.ofHours(1);

    /** 宠物个性化推荐缓存有效期 */
    private Duration petRecommendTtl = Duration.ofMinutes(10);
}
