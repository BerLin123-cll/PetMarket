package com.pet.petmarket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单相关配置
 *
 * <p>对应 application.yml 中的 {@code petmarket.order.*}，支持环境变量覆盖。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "petmarket.order")
public class OrderProperties {

    /** 待支付订单超时时间（分钟）：超时后自动取消订单并回补库存 */
    private int payTimeoutMinutes = 30;

    /** 超时扫描任务执行间隔（毫秒） */
    private long releaseScanIntervalMs = 60_000;

    /** 单次扫描最多处理的订单数量，防止大批量积压时一次占用过多资源 */
    private int releaseBatchSize = 200;

    /** 是否启用超时自动取消任务 */
    private boolean autoReleaseEnabled = true;
}
