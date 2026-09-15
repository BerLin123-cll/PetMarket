package com.pet.petmarket.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端权限校验 —— 端到端（Spring Security 过滤器链 + MVC 拦截器）集成测试
 *
 * <p>这组测试覆盖「真实 HTTP 链路」，用于守住两个容易踩的坑：</p>
 * <ol>
 *   <li>普通用户 Token 访问 {@code /admin/**} 必须 403（原漏洞：任意有效 JWT 都能访问后台）；</li>
 *   <li>Spring Security 过滤器链不能把管理员请求也挡掉
 *       （曾因写成 {@code .requestMatchers("/admin/**").authenticated()} 导致管理员也返回 401）。</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petmarket_security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 登录并取出返回体中的 token */
    private String login(String url, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        assertEquals(200, node.get("code").asInt(), "登录应成功，返回体：" + body);
        String token = node.get("data").get("token").asText();
        assertFalse(token.isBlank(), "登录应返回 token");
        return token;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }

    private int codeOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("code").asInt();
    }

    @Test
    @DisplayName("验收1：管理员 Token 访问 /admin/** 必须成功（过滤链不得把管理员也挡掉）")
    void adminTokenCanAccessAdminApi() throws Exception {
        String adminToken = login("/admin/auth/login", "admin", "admin123");

        MvcResult result = mockMvc.perform(withToken(get("/admin/product/list"), adminToken))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(200, codeOf(result), "管理员访问后台接口应成功，实际返回：" + result.getResponse().getContentAsString());

        // 再验证另一个后台模块，确保不是只有某个路径放行
        mockMvc.perform(withToken(get("/admin/dashboard/stats"), adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("验收2：普通用户 Token 访问 /admin/** 必须 403（本次修复的核心漏洞）")
    void normalUserTokenIsForbiddenOnAdminApi() throws Exception {
        String userToken = login("/api/user/login", "testuser", "123456");

        MvcResult result = mockMvc.perform(withToken(get("/admin/product/list"), userToken))
                .andExpect(status().isForbidden())
                .andReturn();

        assertEquals(403, codeOf(result), "普通用户访问后台接口应返回 403");
    }

    @Test
    @DisplayName("验收3：普通用户 Token 也不能调用管理端写接口（新增商品）")
    void normalUserTokenCannotWriteAdminApi() throws Exception {
        String userToken = login("/api/user/login", "testuser", "123456");

        mockMvc.perform(post("/admin/product/create")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"越权商品\",\"categoryId\":1,\"price\":1,\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("验收4：未携带 Token 访问 /admin/** 返回 401")
    void missingTokenIsUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin/product/list")).andReturn();
        String detail = "status=" + result.getResponse().getStatus()
                + ", body=[" + result.getResponse().getContentAsString() + "]";
        assertEquals(401, result.getResponse().getStatus(), "无 Token 应返回 401；实际 " + detail);
        assertEquals(401, codeOf(result), "无 Token 应返回 401 与统一 JSON 结构；实际 " + detail);
    }

    @Test
    @DisplayName("验收5：伪造/无效 Token 访问 /admin/** 返回 401")
    void invalidTokenIsUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(withToken(get("/admin/product/list"), "not.a.real.token")).andReturn();
        String detail = "status=" + result.getResponse().getStatus()
                + ", body=[" + result.getResponse().getContentAsString() + "]";
        assertEquals(401, result.getResponse().getStatus(), "无效 Token 应返回 401；实际 " + detail);
        assertEquals(401, codeOf(result), "无效 Token 应返回 401；实际 " + detail);
    }

    @Test
    @DisplayName("验收6：后台登录接口允许匿名访问，前台公开接口不受影响")
    void publicEndpointsRemainAccessible() throws Exception {
        // 登录接口匿名可用（能拿到 token 即说明放行）
        assertNotNull(login("/admin/auth/login", "admin", "admin123"));

        mockMvc.perform(get("/api/product/list").param("pageNum", "1").param("pageSize", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("验收7：未登录访问需登录的前台接口返回 401")
    void protectedFrontApiRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/order/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("验收8：登录返回的 Token 载荷中角色声明正确（管理员 true / 普通用户 false）")
    void adminTokenCarriesAdminRole() throws Exception {
        String adminToken = login("/admin/auth/login", "admin", "admin123");
        String adminPayload = new String(java.util.Base64.getUrlDecoder().decode(adminToken.split("\\.")[1]));
        assertTrue(adminPayload.contains("\"isAdmin\":true"), "管理员 Token 应带 isAdmin=true，实际载荷：" + adminPayload);

        String userToken = login("/api/user/login", "testuser", "123456");
        String userPayload = new String(java.util.Base64.getUrlDecoder().decode(userToken.split("\\.")[1]));
        assertTrue(userPayload.contains("\"isAdmin\":false"), "普通用户 Token 应带 isAdmin=false，实际载荷：" + userPayload);
    }
}
