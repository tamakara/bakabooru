package com.tamakara.bakabooru.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class SignatureUtils {

    // 使用 HMAC-SHA256 算法生成签名
    public static String generateSignature(String path, long expiresAt, String secretKey) {
        try {
            String data = path + "?expiresAt=" + expiresAt;

            // 创建 HMAC SHA-256 的签名器
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);

            // 生成签名
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}
