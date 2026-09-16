package com.pet.petmarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 跨域配置
 *
 * <p>允许前端跨域访问后端接口。因为开启了 {@code allowCredentials(true)}，
 * 允许来源<b>不能</b>使用通配符 {@code *}，否则浏览器会直接拒绝且存在安全风险。
 * 这里改为「显式域名白名单」，通过配置项 {@code petmarket.cors.allowed-origins}
 * 维护（逗号分隔），默认放行本地前端开发地址，生产环境用环境变量覆盖为真实域名即可。</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的来源白名单（逗号分隔）。
     * 默认放行 Vite 本地开发地址；生产环境请通过 petmarket.cors.allowed-origins 覆盖为真实前端域名。
     */
    @Value("${petmarket.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
