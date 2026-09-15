package com.pet.petmarket.task;

import com.pet.petmarket.config.OrderProperties;
import com.pet.petmarket.service.OrdersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时自动取消任务
 *
 * <p>待支付订单占用着库存。用户下单后如果一直不付款，库存就被「挂住」了，
 * 因此这里定时扫描超过 {@code petmarket.order.pay-timeout-minutes} 仍未支付的订单，
 * 自动置为「已取消」并把库存回补回去。</p>
 *
 * <p>幂等与并发安全：真正的取消/回补走 {@code OrdersService#releaseTimeoutOrder}，
 * 内部用「条件状态流转（CAS）+ 条件回补」保证同一订单只会释放一次库存，
 * 因此即使与用户手动取消、或者多实例部署同时执行，也不会重复加库存。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "petmarket.order", name = "auto-release-enabled",
        havingValue = "true", matchIfMissing = true)
public class OrderTimeoutTask {

    private final OrdersService ordersService;
    private final OrderProperties orderProperties;

    /**
     * 定时释放超时未支付订单占用的库存
     *
     * <p>间隔由配置项 {@code petmarket.order.release-scan-interval-ms} 决定（默认 60 秒），
     * 也可以用环境变量 ORDER_RELEASE_SCAN_INTERVAL_MS 覆盖。</p>
     */
    @Scheduled(fixedDelayString = "${petmarket.order.release-scan-interval-ms:60000}",
            initialDelayString = "${petmarket.order.release-scan-interval-ms:60000}")
    public void releaseTimeoutOrders() {
        try {
            int released = ordersService.releaseTimeoutOrders(orderProperties.getReleaseBatchSize());
            if (released > 0) {
                log.info("超时未支付订单已自动取消并回补库存：{} 单", released);
            }
        } catch (Exception e) {
            // 定时任务异常必须自行捕获，否则会导致后续调度被取消
            log.error("超时订单释放任务执行异常：{}", e.getMessage(), e);
        }
    }
}
