package com.pet.petmarket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 启动配置校验器
 *
 * <p>发布前最容易踩的坑是把开发用的密钥/密码带上线，因此这里做「快速失败」校验：</p>
 * <ul>
 *   <li>生产 profile（prod）下不允许使用内置的开发默认密钥/默认数据库密码，否则直接终止启动；</li>
 *   <li>JWT 密钥长度不足 32 字节（HS256 最低要求）时直接终止启动；</li>
 *   <li>开发环境仅打印告警，不阻断启动。</li>
 * </ul>
 */
@Slf4j
@Component
public class StartupConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    /** 代码仓库中内置的开发默认密钥（与 application.yml 保持一致） */
    private static final String DEFAULT_DEV_SECRET =
            "petmarket-dev-only-secret-key-please-override-in-production-2024";

    /** 开发默认数据库密码 */
    private static final String DEFAULT_DEV_DB_PASSWORD = "123456";

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.url:}")
    private String dbUrl;

    public StartupConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean prod = isProdProfile();

        // 1. JWT 密钥强度校验（生产 + 开发都校验长度）
        if (jwtSecret == null || jwtSecret.isBlank()) {
            failOrWarn(prod, "JWT 密钥（JWT_SECRET）未配置，请通过环境变量提供");
        } else if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failOrWarn(prod, "JWT 密钥长度不足 32 字节，HS256 要求至少 32 字节，请重新生成");
        }

        if (prod) {
            // 2. 生产环境禁止使用内置默认密钥
            if (DEFAULT_DEV_SECRET.equals(jwtSecret)) {
                failOrWarn(true, "生产环境仍在用代码内置的默认 JWT 密钥，请立即轮换："
                        + "设置环境变量 JWT_SECRET 为一段随机字符串（>=32字节）");
            }
            // 3. 生产环境禁止使用默认数据库密码
            if (DEFAULT_DEV_DB_PASSWORD.equals(dbPassword)) {
                failOrWarn(true, "生产环境仍在用默认数据库密码，请设置环境变量 DB_PASSWORD");
            }
            // 4. 生产环境必须显式指定数据库地址
            if (dbUrl == null || dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
                log.warn("[配置检查] 生产环境数据库地址仍指向本机（{}），请确认是否已通过 DB_URL 覆盖", dbUrl);
            }
            log.info("[配置检查] 生产环境配置校验通过（active profiles: {}）",
                    Arrays.toString(environment.getActiveProfiles()));
        } else {
            log.warn("[配置检查] 当前为开发环境（active profiles: {}），"
                            + "请勿将开发密钥与默认数据库密码用于生产；生产部署请使用 --spring.profiles.active=prod",
                    Arrays.toString(environment.getActiveProfiles()));
        }
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p));
    }

    /**
     * 生产环境直接抛异常终止启动（快速失败）；开发环境只打印告警。
     */
    private void failOrWarn(boolean prod, String message) {
        if (prod) {
            throw new IllegalStateException("[配置检查-生产环境阻断] " + message);
        }
        log.warn("[配置检查] {}", message);
    }
}
