package com.finance.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - 令牌签发与校验
 *
 * @author 胡宪棋
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.admin-expiration}")
    private Long adminExpiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ==================== 普通用户 Token ====================

    /**
     * 签发普通用户JWT令牌
     */
    public String generateUserToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user");
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", "USER");

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // ==================== 管理员 Token ====================

    /**
     * 签发管理员JWT令牌
     */
    public String generateAdminToken(Long adminId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "admin");
        claims.put("adminId", adminId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + adminExpiration))
                .signWith(getKey())
                .compact();
    }

    // ==================== Token 校验与解析 ====================

    /**
     * 解析JWT令牌
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为管理员Token
     */
    public boolean isAdminToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "admin".equals(claims.get("sub"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中提取管理员ID
     */
    public Long getAdminIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("adminId", Long.class);
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中提取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
}
