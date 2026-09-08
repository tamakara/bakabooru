package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.dto.SettingDefinitionDto;
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
    public static final String BOOTSTRAP_DATABASE_URL = "bootstrap.database-url";
    public static final String BOOTSTRAP_STORAGE_ENDPOINT = "bootstrap.storage-endpoint";

    private static final Set<String> EDITABLE_KEYS = Set.of(
            TAG_THRESHOLD,
            AI_MAX_ATTEMPTS,
            AI_RETRY_BASE_DELAY_SECONDS,
            AI_RETRY_MAX_DELAY_SECONDS,
            UPLOAD_COMPLETED_RETENTION_DAYS,
            "ai.service-url",
            "ai.inference-concurrency"
    );

    private final SystemSettingRepository systemSettingRepository;

    public List<SettingDefinitionDto> getDefinitions() {
        return List.of(
                new SettingDefinitionDto(TAG_THRESHOLD, "Tag threshold", "number", "0.61", "HOT", false, "Minimum AI tag confidence"),
                new SettingDefinitionDto(AI_MAX_ATTEMPTS, "AI max attempts", "integer", "5", "HOT", false, "Maximum AI attempts"),
                new SettingDefinitionDto(AI_RETRY_BASE_DELAY_SECONDS, "AI retry base delay", "integer", "30", "HOT", false, "Base retry delay seconds"),
                new SettingDefinitionDto(AI_RETRY_MAX_DELAY_SECONDS, "AI retry max delay", "integer", "1800", "HOT", false, "Maximum retry delay seconds"),
                new SettingDefinitionDto(UPLOAD_COMPLETED_RETENTION_DAYS, "Upload retention", "integer", "7", "HOT", false, "Completed upload retention days"),
                new SettingDefinitionDto("ai.service-url", "AI service URL", "text", "http://ai-service:8000", "HOT", false, "Runtime AI service endpoint"),
                new SettingDefinitionDto("ai.inference-concurrency", "Inference concurrency", "integer", "1", "HOT", false, "Maximum concurrent inference requests"),
                new SettingDefinitionDto("ai.device-mode", "Device mode", "text", "cuda", "RELOAD", false, "Inference is fixed to CUDA"),
                new SettingDefinitionDto("ai.model-cache-dir", "Model cache directory", "text", "/model_cache", "RELOAD", false, "Container-managed AI model cache directory"),
                new SettingDefinitionDto(BOOTSTRAP_DATABASE_URL, "Database URL", "text", "jdbc:postgresql://postgres:5432/bakabooru", "BOOTSTRAP", false, "Database connection URL"),
                new SettingDefinitionDto(BOOTSTRAP_STORAGE_ENDPOINT, "Storage endpoint", "text", "http://minio:9000", "BOOTSTRAP", false, "Storage service endpoint")
        );
    }

    @Transactional(readOnly = true)
    public List<SettingDefinitionDto> getDefinitionsWithCurrentValues() {
        List<SettingDefinitionDto> definitions = new ArrayList<>(getDefinitions());
        Map<String, String> persisted = systemSettingRepository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
        definitions.forEach(definition -> {
            definition.setCurrentValue(persisted.getOrDefault(definition.getKey(), definition.getDefaultValue()));
            definition.setRequiresRestart("BOOTSTRAP".equals(definition.getScope()));
        });
        return definitions;
    }

    /** 返回设置页允许编辑的配置，并使用定义中的默认值补齐缺失项。 */
    @Transactional(readOnly = true)
    public Map<String, String> getEditableSettings() {
        Map<String, String> values = getDefinitions().stream()
                .filter(definition -> EDITABLE_KEYS.contains(definition.getKey()))
                .collect(Collectors.toMap(SettingDefinitionDto::getKey, SettingDefinitionDto::getDefaultValue));
        systemSettingRepository.findAllById(EDITABLE_KEYS).forEach(setting -> values.put(setting.getKey(), setting.getValue()));
        return values;
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElseThrow(() -> new RuntimeException("Setting with key: " + key + " not found"));
    }

    public String getOptionalSetting(String key, String defaultValue) {
        return systemSettingRepository.findById(key).map(SystemSetting::getValue)
                .filter(value -> value != null && !value.isBlank()).orElse(defaultValue);
    }

    // Typed setting accessors used by background workers.

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

    /** 更新单个内部配置。 */
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
        int inferenceConcurrency = parseInt(settings, "ai.inference-concurrency");

        requireRange(TAG_THRESHOLD, threshold, 0, 1);
        requireRange(AI_MAX_ATTEMPTS, maxAttempts, 1, 20);
        requireRange(AI_RETRY_BASE_DELAY_SECONDS, retryBaseDelay, 1, 3600);
        requireRange(UPLOAD_COMPLETED_RETENTION_DAYS, retentionDays, 1, 365);
        requireRange("ai.inference-concurrency", inferenceConcurrency, 1, 64);
        if (retryMaxDelay < retryBaseDelay) {
            throw new IllegalArgumentException(AI_RETRY_MAX_DELAY_SECONDS
                    + " must be greater than or equal to " + AI_RETRY_BASE_DELAY_SECONDS);
        }
        requireValue(settings, "ai.service-url");
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

    /** 批量更新已经存在的内部配置。 */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>(newSettings.keySet());

        List<SystemSetting> existingSettings = systemSettingRepository.findAllById(keys);

        if (existingSettings.size() != keys.size()) {
            Set<String> existingKeys = existingSettings.stream()
                    .map(SystemSetting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        for (SystemSetting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        systemSettingRepository.saveAll(existingSettings);

    }
}
