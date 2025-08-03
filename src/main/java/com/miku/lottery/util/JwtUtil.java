package com.miku.lottery.util;

import io.jsonwebtoken.Claims; // 导入 JWT Claims 类
import io.jsonwebtoken.Jwts; // 导入 JWT 工具类
import io.jsonwebtoken.SignatureAlgorithm; // 导入签名算法类
import io.jsonwebtoken.security.Keys; // 导入 Keys 工具类，用于生成安全密钥

import java.security.Key; // 导入 Key 接口
import java.util.Date; // 导入 Date 类
import java.util.HashMap; // 导入 HashMap
import java.util.Map; // 导入 Map

/**
 * JWT (JSON Web Token) 工具类
 * 负责 JWT 的生成、解析和验证。
 */
public class JwtUtil {

    // JWT 签名密钥，生产环境请务必使用强随机字符串，并妥善保管
    // 这里为了演示方便，直接硬编码，实际应用中应从配置文件读取或更安全的方式获取
    private static final String SECRET_KEY_STRING = "your-very-secure-secret-key-for-jwt-signing-purpose-please-change-this-in-production";
    // 将字符串密钥转换为 Java Security Key 对象
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    // JWT 有效期（毫秒），这里设置为 1 小时
    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1小时

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @param userId 用户ID
     * @return 生成的 JWT 字符串
     */
    public static String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        // 将用户ID和用户名作为 payload (载荷) 中的 claims (声明)
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims) // 设置自定义声明
                .setSubject(username) // 设置主题（通常是用户标识）
                .setIssuedAt(new Date(System.currentTimeMillis())) // 设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 设置过期时间
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // 使用 HS256 算法和密钥进行签名
                .compact(); // 压缩生成 JWT 字符串
    }

    /**
     * 从 JWT Token 中解析出所有声明 (Claims)
     *
     * @param token JWT 字符串
     * @return Claims 对象，包含所有声明信息
     */
    public static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY) // 设置签名密钥
                .build()
                .parseClaimsJws(token) // 解析 JWS (JSON Web Signature)
                .getBody(); // 获取 Claims (载荷)
    }

    /**
     * 从 JWT Token 中提取用户名
     *
     * @param token JWT 字符串
     * @return 用户名
     */
    public static String extractUsername(String token) {
        return extractAllClaims(token).getSubject(); // 主题就是用户名
    }

    /**
     * 从 JWT Token 中提取用户ID
     *
     * @param token JWT 字符串
     * @return 用户ID
     */
    public static Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class); // 从声明中获取 userId
    }

    /**
     * 验证 JWT Token 是否有效且未过期
     *
     * @param token JWT 字符串
     * @return true 如果 Token 有效且未过期，否则 false
     */
    public static boolean validateToken(String token) {
        try {
            // 解析 Token，如果解析过程中发生异常（如签名不匹配、Token 过期），则验证失败
            extractAllClaims(token);
            return true; // 解析成功，说明 Token 有效
        } catch (Exception e) {
            // Token 过期、签名错误等都会抛出异常
            System.err.println("JWT Token 验证失败: " + e.getMessage());
            return false;
        }
    }
}
