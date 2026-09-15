package com.pet.petmarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器
 *
 * <p>拦截需要登录的接口，验证 Token 有效性，并把 userId / isAdmin 放入请求属性供控制器使用。</p>
 *
 * <p><b>注意</b>：拒绝访问时必须同时设置 HTTP 状态码（401）与响应体，只写响应体不设状态码时
 * HTTP 状态仍是 200，前端 axios 的 401 分支不会触发，会被当成本地业务失败处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 请求属性名：当前登录用户ID */
    public static final String ATTR_USER_ID = "userId";

    /** 请求属性名：当前登录用户是否为管理员 */
    public static final String ATTR_IS_ADMIN = "isAdmin";

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 统一从请求头解析 Token（自动去除 Bearer 前缀）
        String token = jwtUtils.resolveToken(request);

        if (token == null) {
            log.warn("请求缺少 Token：uri={}", request.getRequestURI());
            return reject(response);
        }

        try {
            if (!jwtUtils.validateToken(token)) {
                log.warn("Token 无效或已过期：uri={}", request.getRequestURI());
                return reject(response);
            }

            // 将用户信息存入请求属性，供后续使用
            Long userId = jwtUtils.getUserIdFromToken(token);
            request.setAttribute(ATTR_USER_ID, userId);
            request.setAttribute(ATTR_IS_ADMIN, jwtUtils.isAdminFromToken(token));

            return true;
        } catch (Exception e) {
            log.error("Token验证失败：{}", e.getMessage());
            return reject(response);
        }
    }

    /**
     * 输出统一的 401 JSON 响应（同时设置 HTTP 状态码 401）
     */
    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(ResultCode.UNAUTHORIZED.getCode());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage())));
        return false;
    }
}
