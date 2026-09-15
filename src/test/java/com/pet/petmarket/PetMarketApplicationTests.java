package com.pet.petmarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文启动测试
 *
 * <p>使用独立 H2 内存库，避免测试依赖本机 MySQL；数据源来自
 * {@code src/test/resources/application.yml}。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_context;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class PetMarketApplicationTests {

    @Test
    void contextLoads() {
    }

}
