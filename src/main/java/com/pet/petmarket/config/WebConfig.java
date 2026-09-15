package com.pet.petmarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置类
 * 注册拦截器（认证 + 管理端角色校验），并映射本地上传目录为静态资源
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminGuardInterceptor adminGuardInterceptor;

    /**
     * 本地上传配置（上传根目录 + 访问前缀）
     */
    private final UploadProperties uploadProperties;

    /** 管理端无需登录的接口 */
    private static final String[] ADMIN_WHITELIST = {
            "/admin/auth/login"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 登录态校验：前台接口 + 后台接口都需要有效 Token
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/admin/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/product/**",
                        "/api/category/**",
                        "/admin/auth/login"
                )
                .order(1);

        // 2. 管理端角色校验：/admin/** 必须是管理员 Token（普通用户 403）
        registry.addInterceptor(adminGuardInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(ADMIN_WHITELIST)
                .order(2);
    }

    /**
     * 把本地上传目录映射为可访问的静态资源。
     * 例如 uploadProperties.urlPrefix = /uploads，本地目录 = ./data/upload，
     * 上传文件 data/upload/product/xxx.jpg 可通过 /uploads/product/xxx.jpg 访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler(uploadProperties.getUrlPrefix() + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
