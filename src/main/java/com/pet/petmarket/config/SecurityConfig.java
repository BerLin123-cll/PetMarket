package com.pet.petmarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

/**
 * Spring Security 配置类
 *
 * <p>本项目采用「Spring Security 过滤器链做路径级放行 + MVC 拦截器做 JWT/角色校验」的双层策略：</p>
 * <ul>
 *   <li>Spring Security 负责关闭 CSRF、关闭 Session、关闭默认登录页，并统一 401/403 的 JSON 响应格式；</li>
 *   <li>{@link AuthInterceptor} 负责登录态校验，{@link AdminGuardInterceptor} 负责管理员角色校验。</li>
 * </ul>
 *
 * <p><b>重要说明</b>：本项目没有配置任何 Spring Security 认证过滤器（登录走自定义 JWT + 拦截器），
 * 因此过滤器链里不存在已认证的 Authentication。此时若写成
 * {@code .requestMatchers("/admin/**").authenticated()}，由于匿名请求永远无法满足该条件，
 * 会把**管理员请求也一起挡掉（返回 401）**，属于典型配置陷阱。
 * 所以过滤器链只做 permitAll（安全由拦截器保证），真正的角色校验见 {@link AdminGuardInterceptor}。</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder matcher = PathPatternRequestMatcher.withDefaults();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 过滤器链不做认证拦截（无 Spring Security 认证过滤器，拦截器负责 JWT + 角色校验）。
                        // 这里保留显式规则是为了让放行范围一目了然，且避免将来误加规则导致管理员请求被 401。
                        .requestMatchers(matcher.matcher("/admin/auth/login")).permitAll()
                        .requestMatchers(matcher.matcher("/admin/**")).permitAll()
                        .requestMatchers(
                                matcher.matcher("/api/user/login"),
                                matcher.matcher("/api/user/register"),
                                matcher.matcher("/api/product/**"),
                                matcher.matcher("/api/category/**")
                        ).permitAll()
                        .requestMatchers(
                                matcher.matcher("/uploads/**"),
                                matcher.matcher("/images/**"),
                                matcher.matcher("/error")
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                // 未认证 / 无权限统一返回 JSON，避免重定向到登录页
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, ResultCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, ResultCode.FORBIDDEN))
                );
        return http.build();
    }

    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(resultCode.getCode(), resultCode.getMessage())));
    }
}
