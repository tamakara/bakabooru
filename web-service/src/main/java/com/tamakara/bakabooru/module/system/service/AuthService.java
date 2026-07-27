package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemSettingService systemSettingService;

    private String getEncodedPassword() {
        return systemSettingService.getSetting("system.auth-password");
    }

    public boolean isPasswordSet() {
        String password = getEncodedPassword();
        return StringUtils.hasText(password);
    }

    public boolean isInitialized() {
        return systemSettingService.getBooleanSetting("system.auth-initialized");
    }

    public String login(String password) {
        // 简单解码 Base64 存储的密码 (注意: 生产环境应使用 BCrypt 等哈希算法)
        String storedEncoded = getEncodedPassword();
        String currentPassword = decodePassword(storedEncoded);

        if (!currentPassword.equals(password)) {
            throw new RuntimeException("密码错误");
        }

        // 生成 Token，有效期 24小时
        return JwtUtils.createToken(JwtUtils.generateSecretKey(currentPassword), 1000 * 60 * 60 * 24);
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        String encoded = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));

        Map<String, String> settings = new HashMap<>();
        settings.put("system.auth-password", encoded);
        settings.put("system.auth-initialized", "true");

        systemSettingService.updateSettings(settings);
    }

    public void validate(String token) {
        if (!isInitialized()) return;

        String currentPassword = decodePassword(getEncodedPassword());
        if (!StringUtils.hasText(currentPassword)) return;

        if (JwtUtils.isTokenExpired(token, JwtUtils.generateSecretKey(currentPassword))) {
            throw new RuntimeException("Token已过期或无效");
        }
    }

    private String decodePassword(String encoded) {
        if (!StringUtils.hasText(encoded)) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
