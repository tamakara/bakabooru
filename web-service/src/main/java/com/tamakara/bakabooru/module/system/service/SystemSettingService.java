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

    public static final String TAG_THRESHOLD = "tag.threshold";
    public static final String AI_MAX_ATTEMPTS = "ai-job.max-attempts";
    public static final String AI_RETRY_BASE_DELAY_SECONDS = "ai-job.retry-base-delay-seconds";
    public static final String AI_RETRY_MAX_DELAY_SECONDS = "ai-job.retry-max-delay-seconds";
    public static final String UPLOAD_COMPLETED_RETENTION_DAYS = "upload.completed-retention-days";

    private static final Set<String> EDITABLE_KEYS = Set.of(
            TAG_THRESHOLD,
            AI_MAX_ATTEMPTS,
            AI_RETRY_BASE_DELAY_SECONDS,
            AI_RETRY_MAX_DELAY_SECONDS,
            UPLOAD_COMPLETED_RETENTION_DAYS
    );

    private final SystemSettingRepository systemSettingRepository;

    /**
     * 获取所有配置。
     */
    @Transactional(readOnly = true)
    public Map<String, String> getEditableSettings() {
        return systemSettingRepository.findAllById(EDITABLE_KEYS).stream()
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

    public int getAiMaxAttempts() {
        return getIntSetting(AI_MAX_ATTEMPTS);
    }

    public long getAiRetryBaseDelaySeconds() {
        return getLongSetting(AI_RETRY_BASE_DELAY_SECONDS);
    }

    public long getAiRetryMaxDelaySeconds() {
        return getLongSetting(AI_RETRY_MAX_DELAY_SECONDS);
    }

    public long getUploadCompletedRetentionDays() {
        return getLongSetting(UPLOAD_COMPLETED_RETENTION_DAYS);
    }

    /**
     * 单条更新
     */
    @Transactional
    public void updateSetting(String key, String value) {
        updateSettings(Collections.singletonMap(key, value));
    }

    @Transactional
    public void updateEditableSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> unknownKeys = new HashSet<>(newSettings.keySet());
        unknownKeys.removeAll(EDITABLE_KEYS);
        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException("Unknown or read-only settings: " + unknownKeys);
        }

        Map<String, String> effectiveSettings = new HashMap<>(getEditableSettings());
        effectiveSettings.putAll(newSettings);
        validateEditableSettings(effectiveSettings);
        updateSettings(newSettings);
    }

    private void validateEditableSettings(Map<String, String> settings) {
        double threshold = parseDouble(settings, TAG_THRESHOLD);
        int maxAttempts = parseInt(settings, AI_MAX_ATTEMPTS);
        long retryBaseDelay = parseLong(settings, AI_RETRY_BASE_DELAY_SECONDS);
        long retryMaxDelay = parseLong(settings, AI_RETRY_MAX_DELAY_SECONDS);
        long retentionDays = parseLong(settings, UPLOAD_COMPLETED_RETENTION_DAYS);

        requireRange(TAG_THRESHOLD, threshold, 0.0, 1.0);
        requireRange(AI_MAX_ATTEMPTS, maxAttempts, 1, 20);
        requireRange(AI_RETRY_BASE_DELAY_SECONDS, retryBaseDelay, 1, 3600);
        requireRange(AI_RETRY_MAX_DELAY_SECONDS, retryMaxDelay, 1, 86400);
        requireRange(UPLOAD_COMPLETED_RETENTION_DAYS, retentionDays, 1, 365);
        if (retryMaxDelay < retryBaseDelay) {
            throw new IllegalArgumentException(AI_RETRY_MAX_DELAY_SECONDS
                    + " must be greater than or equal to " + AI_RETRY_BASE_DELAY_SECONDS);
        }
    }

    private int parseInt(Map<String, String> settings, String key) {
        try {
            return Integer.parseInt(requireValue(settings, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer", error);
        }
    }

    private long parseLong(Map<String, String> settings, String key) {
        try {
            return Long.parseLong(requireValue(settings, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer", error);
        }
    }

    private double parseDouble(Map<String, String> settings, String key) {
        try {
            return Double.parseDouble(requireValue(settings, key));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be a number", error);
        }
    }

    private String requireValue(Map<String, String> settings, String key) {
        String value = settings.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.trim();
    }

    private void requireRange(String key, double value, double minimum, double maximum) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
        }
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
