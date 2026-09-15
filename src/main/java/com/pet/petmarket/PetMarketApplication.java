package com.pet.petmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PetMarket 萌宠商城启动类
 *
 * <p>{@code @EnableScheduling} 用于开启定时任务，支撑「待支付订单超时自动取消并回补库存」。</p>
 */
@EnableScheduling
@SpringBootApplication
public class PetMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetMarketApplication.class, args);
    }

}
