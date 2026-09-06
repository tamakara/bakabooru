package com.tamakara.bakabooru.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

public class JwtUtils {

    // 閻㈢喐鍨?SecretKey
    public static SecretKey generateSecretKey(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // 閻㈢喐鍨?Token
    public static String createToken(SecretKey secretKey, long expirationTime) {
        return Jwts.builder()
                .header().add("typ", "JWT").add("alg", "HS256").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    // 鐟欙絾鐎介獮鑸电墡妤?Token
    public static Claims parseToken(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey) // 妤犲矁鐦夌粵鎯ф倳
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 閸掋倖鏌?Token 閺勵垰鎯佹潻鍥ㄦ埂
    public static boolean isTokenExpired(String token, SecretKey secretKey) {
        try {
            Claims claims = parseToken(token, secretKey);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // 鐟欙絾鐎芥径杈Е閸楀疇顫嬫稉楦跨箖閺堢喐鍨ㄩ弮鐘虫櫏
        }
    }
}
