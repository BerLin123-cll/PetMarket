package com.pet.petmarket.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 负责生成、解析、验证 JWT Token
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.prefix:Bearer}")
    private String prefix;

    /**
     * 从请求头中解析 Token（去除 Bearer 前缀）
     *
     * @return Token 字符串；请求头缺失或为空时返回 null
     */
    public String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (token == null || token.isBlank()) {
            return null;
        }
        token = token.trim();
        String prefixWithSpace = prefix + " ";
        if (token.startsWith(prefixWithSpace)) {
            token = token.substring(prefixWithSpace.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }

    /**
     * Token 有效期（毫秒）
     */
    public Long getExpiration() {
        return expiration;
    }

    /**
     * 生成密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Token
     * @param userId 用户ID
     * @param username 用户名
     * @param isAdmin 是否为管理员
     * @return JWT Token
     */
    public String generateToken(Long userId, String username, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("isAdmin", isAdmin);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("JWT解析失败：{}", e.getMessage());
            throw new RuntimeException("Token无效或已过期");
        }
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 判断是否为管理员
     */
    public Boolean isAdminFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("isAdmin", Boolean.class);
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
