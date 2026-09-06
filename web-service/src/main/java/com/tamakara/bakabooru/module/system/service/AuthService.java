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
        // 缂備胶濮崑鎾绘煕濡や焦绀夌悮娆撴煟?Base64 闁诲孩绋掗敋闁稿绉归幆鍐礋椤愩倖顫氶梺?(濠电偛顦崝宥夊礈? 闂佹眹鍨婚崰宥嗩殽閸ヮ剚鍋濇い鏍ㄥ嚬閺嗘棃骞栫€涙ɑ鐓ｅ┑鐐叉喘閹?BCrypt 缂備焦绋戦ˇ顖炲箹鏉堚晜鏆滅€光偓閳ь剟鎮剧拠娴嬫灃?
        String storedEncoded = getEncodedPassword();
        String currentPassword = decodePassword(storedEncoded);

        if (!currentPassword.equals(password)) {
            throw new RuntimeException("Authentication failed");
        }

        // 闂佹眹鍨婚崰鎰板垂?Token闂佹寧绋戦張顒€锕㈡笟鈧顐﹀醇閻旂鐒?24闁诲繐绻愮换鎴濐渻?
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
            throw new RuntimeException("Authentication failed");
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
