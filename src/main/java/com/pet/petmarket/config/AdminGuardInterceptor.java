package com.pet.petmarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.petmarket.common.Result;
import com.pet.petmarket.common.ResultCode;
import com.pet.petmarket.entity.Admin;
import com.pet.petmarket.service.AdminService;
import com.pet.petmarket.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理端权限守卫拦截器
 *
 * <p>用于把「登录态」与「管理员角色」彻底分开：仅有 AuthInterceptor 时，任意有效用户
 * Token 都能访问 {@code /admin/**}。本拦截器在 AuthInterceptor 之后执行，三重校验：</p>
 * <ol>
 *   <li>Token 有效（签名 + 未过期）；</li>
 *   <li>Token 中的 {@code isAdmin} 声明为 true（角色校验，普通用户直接 403）；</li>
 *   <li>管理员账号在库中仍然存在（防止已删除的管理员继续使用旧 Token）。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminGuardInterceptor implements HandlerInterceptor {

    /** 请求属性名：当前登录管理员ID */
    public static final String ATTR_ADMIN_ID = "adminId";

    /** 请求属性名：当前登录管理员用户名 */
    public static final String ATTR_ADMIN_NAME = "adminUsername";

    private final JwtUtils jwtUtils;
    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行，避免跨域探测被拦截
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = jwtUtils.resolveToken(request);
        log.debug("[AdminGuard] uri={}, token解析结果={}", request.getRequestURI(), token == null ? "null" : "非空");

        // 1. 无 Token：401，提示先登录
        if (token == null) {
            log.warn("[AdminGuard] 缺少 Token，拒绝访问：uri={}", request.getRequestURI());
            return reject(response, ResultCode.UNAUTHORIZED);
        }

        // 2. Token 非法/过期：401
        if (!jwtUtils.validateToken(token)) {
            log.warn("[AdminGuard] Token 校验失败（签名或过期），拒绝访问：uri={}", request.getRequestURI());
            return reject(response, ResultCode.UNAUTHORIZED);
        }

        // 3. 角色校验：非管理员一律 403
        Boolean isAdmin = jwtUtils.isAdminFromToken(token);
        if (!Boolean.TRUE.equals(isAdmin)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            log.warn("普通用户尝试访问管理端接口：uri={}, userId={}, isAdmin={}",
                    request.getRequestURI(), userId, isAdmin);
            return reject(response, ResultCode.FORBIDDEN);
        }

        // 4. 账号存在性校验：管理员被删除后旧 Token 立即失效
        Long adminId = jwtUtils.getUserIdFromToken(token);
        Admin admin = adminId == null ? null : adminService.getById(adminId);
        if (admin == null) {
            log.warn("管理员账号不存在或已被删除：uri={}, adminId={}", request.getRequestURI(), adminId);
            return reject(response, ResultCode.FORBIDDEN);
        }

        request.setAttribute(ATTR_ADMIN_ID, admin.getId());
        request.setAttribute(ATTR_ADMIN_NAME, admin.getUsername());
        return true;
    }

    /**
     * 输出统一的 JSON 拒绝响应，同时设置 HTTP 状态码，
     * 便于前端 axios 响应拦截器按 401/403 分流处理。
     */
    private boolean reject(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getCode());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(resultCode.getCode(), resultCode.getMessage())));
        return false;
    }
}
