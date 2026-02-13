package com.tamakara.bakabooru.module.file.service;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.utils.SignatureUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SignatureService {

    private final SystemSettingService systemSettingService;

    private String getEncodedPassword() {
        return systemSettingService.getSetting("auth.password");
    }

    public String generateSignedUrl(String path, long expires) {
        String storedEncodedPassword = getEncodedPassword();
        long expiresAt = Instant.now().getEpochSecond() + expires;
        // 生成签名
        String signature = SignatureUtils.generateSignature(path, expiresAt, storedEncodedPassword);
        // 生成带签名的 URL
        return path + "?expiresAt=" + expiresAt + "&signature=" + signature;
    }

    public boolean validateSignature(String path, long expiresAt, String signature) {
        // 检查过期时间
        long currentTime = Instant.now().getEpochSecond(); // 当前时间
        // 如果过期时间小于当前时间，则 URL 已经过期
        if (expiresAt < currentTime) {
            return false;
        }

        String storedEncodedPassword = getEncodedPassword();
        // 重新计算签名
        String expectedSignature = SignatureUtils.generateSignature(path, expiresAt, storedEncodedPassword);
        // 比较签名
        return expectedSignature.equals(signature);
    }
}
