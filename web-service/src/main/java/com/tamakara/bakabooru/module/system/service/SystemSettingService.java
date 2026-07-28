package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * 获取所有配置。
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElseThrow(() -> new RuntimeException("Setting with key: " + key + " not found"));
    }

    // --- 类型转换辅助方法 ---

    public Boolean getBooleanSetting(String key) {
        return "true".equalsIgnoreCase(getSetting(key));
    }

    public int getIntSetting(String key) {
        return Integer.parseInt(getSetting(key));
    }

    public long getLongSetting(String key) {
        return Long.parseLong(getSetting(key));
    }

    public double getDoubleSetting(String key) {
        return Double.parseDouble(getSetting(key));
    }

    /**
     * 单条更新
     */
    @Transactional
    public void updateSetting(String key, String value) {
        updateSettings(Collections.singletonMap(key, value));
    }

    /**
     * 批量更新 (深度优化版)
     */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>(newSettings.keySet());

        // 1. 批量查询 DB (1次 SQL: SELECT * FROM table WHERE id IN (...))
        List<SystemSetting> existingSettings = systemSettingRepository.findAllById(keys);

        // 2. 严格检查：确保所有 Key 都存在于数据库中
        if (existingSettings.size() != keys.size()) {
            // 找出哪个 Key 不存在 (可选，用于报错提示)
            Set<String> existingKeys = existingSettings.stream()
                    .map(SystemSetting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        // 3. 在内存中更新 Entity 对象
        for (SystemSetting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        // 4. 批量保存到 DB (1次 SQL 交互，取决于 JPA 实现，通常是 batch update)
        systemSettingRepository.saveAll(existingSettings);

    }
}
