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
        return systemSettingService.getSetting("auth.password");
    }

    public boolean isPasswordSet() {
        String password = getEncodedPassword();
        return StringUtils.hasText(password);
    }

    public boolean isInitialized() {
        return systemSettingService.getBooleanSetting("auth.initialized");
    }

    public String login(String password) {
        String storedEncodedPassword = getEncodedPassword();
        String currentPassword = "";

        if (StringUtils.hasText(storedEncodedPassword)) {
            try {
                currentPassword = new String(Base64.getDecoder().decode(storedEncodedPassword), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                currentPassword = "";
            }
        }

        if (!currentPassword.equals(password)) {
            throw new RuntimeException("密码错误");
        }

        return JwtUtils.createToken(JwtUtils.generateSecretKey(currentPassword), 1000 * 60 * 60 * 24); //TODO: 自定义过期时间
    }

    public void setPassword(String password) {
        if (password == null) password = "";
        String encoded = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        Map<String, String> map = new HashMap<>();
        map.put("auth.password", encoded);
        map.put("auth.initialized", "true");
        systemSettingService.updateSettings(map);
    }

    public void validate(String token) {
        if (!isInitialized()) return;

        String storedEncodedPassword = getEncodedPassword();

        String currentPassword = "";
        if (StringUtils.hasText(storedEncodedPassword)) {
            try {
                currentPassword = new String(Base64.getDecoder().decode(storedEncodedPassword), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                currentPassword = "";
            }
        }

        if (!StringUtils.hasText(currentPassword)) {
            return;
        }

        if (JwtUtils.isTokenExpired(token, JwtUtils.generateSecretKey(currentPassword))) {
            throw new RuntimeException("Token已过期 or 无效");
        }
    }
}
