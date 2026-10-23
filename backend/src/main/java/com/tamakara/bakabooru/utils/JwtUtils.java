package com.tamakara.bakabooru.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

public class JwtUtils {

    // 从密码派生签名密钥。
    public static SecretKey generateSecretKey(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // 创建 JWT。
    public static String createToken(SecretKey secretKey, long expirationTime) {
        return Jwts.builder()
                .header().add("typ", "JWT").add("alg", "HS256").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    // 解析并验证 JWT。
    public static Claims parseToken(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 判断 JWT 是否已过期或无效。
    public static boolean isTokenExpired(String token, SecretKey secretKey) {
        try {
            Claims claims = parseToken(token, secretKey);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
