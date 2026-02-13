package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    // 将设置缓存在内存中
    private final Map<String, String> settingsCache = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        List<SystemSetting> all = systemSettingRepository.findAll();
        settingsCache.clear();
        for (SystemSetting setting : all) {
            settingsCache.put(setting.getKey(), setting.getValue());
        }
    }

    public String getSetting(String key) {
        return settingsCache.get(key);
    }

    public Boolean getBooleanSetting(String key) {
        String val = settingsCache.get(key);
        return "true".equalsIgnoreCase(val);
    }

    public int getIntSetting(String key) {
        String val = settingsCache.get(key);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer setting for key: " + key);
        }
    }

    public long getLongSetting(String key) {
        String val = settingsCache.get(key);
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid long setting for key: " + key);
        }
    }

    public double getDoubleSetting(String key) {
        String val = settingsCache.get(key);
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid double setting for key: " + key);
        }
    }

    public Map<String, String> getAllSettings() {
        return new HashMap<>(settingsCache);
    }

    @Transactional
    public void updateSetting(String key, String value) {
        SystemSetting setting = new SystemSetting(key, value);
        systemSettingRepository.save(setting);
        settingsCache.put(key, value);
    }

    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            updateSetting(entry.getKey(), entry.getValue());
        }
    }

    @Transactional
    public void resetSettings() {
        Map<String, String> defaultSettings = new HashMap<>();
        defaultSettings.put("upload.allowed-extensions", "jpg,png,webp,gif,jpeg");
        defaultSettings.put("upload.concurrency", "3");
        defaultSettings.put("upload.poll-interval", "1000");
        defaultSettings.put("file.thumbnail.quality", "80");
        defaultSettings.put("file.thumbnail.max-size", "800");
        defaultSettings.put("tag.threshold", "0.6");
        defaultSettings.put("llm.url", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        defaultSettings.put("llm.model", "deepseek-v3.2");
        defaultSettings.put("llm.api-key", "");
        updateSettings(defaultSettings);
        refreshCache();
    }
}
