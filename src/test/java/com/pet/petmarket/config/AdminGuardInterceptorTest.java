package com.pet.petmarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.petmarket.entity.Admin;
import com.pet.petmarket.service.AdminService;
import com.pet.petmarket.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 管理端权限校验拦截器测试
 *
 * <p>验收点：修复「任意有效用户 Token 都能访问 /admin/**」的问题 ——
 * 普通用户 Token 必须被 403 拒绝，管理员 Token 才允许通过。</p>
 */
class AdminGuardInterceptorTest {

    private static final String USER_TOKEN = "user.token.value";
    private static final String ADMIN_TOKEN = "admin.token.value";

    private JwtUtils jwtUtils;
    private AdminService adminService;
    private AdminGuardInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        adminService = mock(AdminService.class);
        interceptor = new AdminGuardInterceptor(jwtUtils, adminService, new ObjectMapper());
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/product/list");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    @Test
    @DisplayName("验收1：未携带 Token 访问 /admin/** 返回 401")
    void rejectWithoutToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(requestWithToken(null), response, new Object());

        assertFalse(allowed, "无 Token 必须被拒绝");
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"), response.getContentAsString());
    }

    @Test
    @DisplayName("验收2：普通用户 Token（isAdmin=false）访问 /admin/** 返回 403（本次修复的核心）")
    void rejectNormalUserToken() throws Exception {
        when(jwtUtils.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn(USER_TOKEN);
        when(jwtUtils.validateToken(USER_TOKEN)).thenReturn(true);
        when(jwtUtils.isAdminFromToken(USER_TOKEN)).thenReturn(false);
        when(jwtUtils.getUserIdFromToken(USER_TOKEN)).thenReturn(100L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(requestWithToken(USER_TOKEN), response, new Object());

        assertFalse(allowed, "普通用户 Token 必须被拒绝");
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":403"), response.getContentAsString());
    }

    @Test
    @DisplayName("验收3：Token 中缺少 isAdmin 声明（null）同样按 403 拒绝")
    void rejectWhenIsAdminClaimMissing() throws Exception {
        when(jwtUtils.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn(USER_TOKEN);
        when(jwtUtils.validateToken(USER_TOKEN)).thenReturn(true);
        when(jwtUtils.isAdminFromToken(USER_TOKEN)).thenReturn(null);
        when(jwtUtils.getUserIdFromToken(USER_TOKEN)).thenReturn(100L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(requestWithToken(USER_TOKEN), response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("验收4：无效/过期 Token 返回 401")
    void rejectInvalidToken() throws Exception {
        when(jwtUtils.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn(USER_TOKEN);
        when(jwtUtils.validateToken(USER_TOKEN)).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(requestWithToken(USER_TOKEN), response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("验收5：管理员 Token（isAdmin=true）且账号存在时放行，并注入 adminId 属性")
    void allowAdminToken() throws Exception {
        when(jwtUtils.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn(ADMIN_TOKEN);
        when(jwtUtils.validateToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtUtils.isAdminFromToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(ADMIN_TOKEN)).thenReturn(1L);

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        when(adminService.getById(1L)).thenReturn(admin);

        MockHttpServletRequest request = requestWithToken(ADMIN_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed, "合法管理员 Token 必须放行");
        assertEquals(200, response.getStatus());
        assertEquals(1L, request.getAttribute(AdminGuardInterceptor.ATTR_ADMIN_ID));
        assertEquals("admin", request.getAttribute(AdminGuardInterceptor.ATTR_ADMIN_NAME));
    }

    @Test
    @DisplayName("验收6：管理员 Token 对应的账号已被删除时返回 403（旧 Token 立即失效）")
    void rejectDeletedAdmin() throws Exception {
        when(jwtUtils.resolveToken(org.mockito.ArgumentMatchers.any())).thenReturn(ADMIN_TOKEN);
        when(jwtUtils.validateToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtUtils.isAdminFromToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(ADMIN_TOKEN)).thenReturn(999L);
        when(adminService.getById(999L)).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(requestWithToken(ADMIN_TOKEN), response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("验收7：OPTIONS 预检请求直接放行，避免跨域探测被拦截")
    void allowOptionsPreflight() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/product/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}
