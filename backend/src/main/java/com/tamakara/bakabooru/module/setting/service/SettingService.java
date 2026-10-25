package com.tamakara.bakabooru.module.setting.service;

import com.tamakara.bakabooru.module.setting.entity.Setting;
import com.tamakara.bakabooru.module.setting.dto.SettingDefinitionDto;
import com.tamakara.bakabooru.module.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingService {

    public static final String TAG_THRESHOLD = "analysis.tag-threshold";
    public static final String ANALYSIS_MAX_ATTEMPTS = "analysis.max-attempts";
    public static final String ANALYSIS_RETRY_BASE_DELAY_SECONDS = "analysis.retry-base-delay-seconds";
    public static final String ANALYSIS_RETRY_MAX_DELAY_SECONDS = "analysis.retry-max-delay-seconds";
    public static final String UPLOAD_COMPLETED_RETENTION_DAYS = "upload.completed-retention-days";
    public static final String INFERENCE_CONCURRENCY = "inference.concurrency";
    public static final String ACTIVE_TAGGER = "model.active-tagger-id";
    public static final String ACTIVE_CLIP = "model.active-clip-id";
    public static final String BOOTSTRAP_DATABASE_URL = "bootstrap.database-url";
    public static final String BOOTSTRAP_STORAGE_ENDPOINT = "bootstrap.storage-endpoint";

    private static final Set<String> EDITABLE_KEYS = Set.of(
            TAG_THRESHOLD,
            ANALYSIS_MAX_ATTEMPTS,
            ANALYSIS_RETRY_BASE_DELAY_SECONDS,
            ANALYSIS_RETRY_MAX_DELAY_SECONDS,
            UPLOAD_COMPLETED_RETENTION_DAYS,
            INFERENCE_CONCURRENCY
    );

    private final SettingRepository settingRepository;

    public List<SettingDefinitionDto> getDefinitions() {
        return List.of(
                new SettingDefinitionDto(TAG_THRESHOLD, "Tag threshold", "number", "0.61", "HOT", false, "Minimum AI tag confidence"),
                new SettingDefinitionDto(ANALYSIS_MAX_ATTEMPTS, "AI max attempts", "integer", "5", "HOT", false, "Maximum AI attempts"),
                new SettingDefinitionDto(ANALYSIS_RETRY_BASE_DELAY_SECONDS, "AI retry base delay", "integer", "30", "HOT", false, "Base retry delay seconds"),
                new SettingDefinitionDto(ANALYSIS_RETRY_MAX_DELAY_SECONDS, "AI retry max delay", "integer", "1800", "HOT", false, "Maximum retry delay seconds"),
                new SettingDefinitionDto(UPLOAD_COMPLETED_RETENTION_DAYS, "Upload retention", "integer", "7", "HOT", false, "Completed upload retention days"),
                new SettingDefinitionDto(INFERENCE_CONCURRENCY, "Inference concurrency", "integer", "1", "HOT", false, "Maximum concurrent inference requests")
        );
    }

    @Transactional(readOnly = true)
    public List<SettingDefinitionDto> getDefinitionsWithCurrentValues() {
        List<SettingDefinitionDto> definitions = new ArrayList<>(getDefinitions());
        definitions.forEach(definition -> {
            definition.setCurrentValue(getValue(definition.getKey(), definition.getDefaultValue()));
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
        EDITABLE_KEYS.forEach(key -> getOptionalSetting(key, null).ifPresent(value -> values.put(key, value)));
        return values;
    }

    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return getOptionalSetting(key, null).orElseThrow(() -> new RuntimeException("Setting with key: " + key + " not found"));
    }

    private final StringRedisTemplate redisTemplate;

    public Optional<String> getOptionalSetting(String key, String defaultValue) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey(key));
            if (cached != null && !cached.isBlank()) return Optional.of(cached);
        } catch (RuntimeException ignored) {
            // PostgreSQL remains available as the source of truth during Redis outages.
        }
        Optional<String> value = settingRepository.findById(key).map(Setting::getValue)
                .filter(item -> item != null && !item.isBlank());
        value.ifPresent(item -> {
            try { redisTemplate.opsForValue().set(cacheKey(key), item); }
            catch (RuntimeException ignored) { /* Cache failure does not fail configuration reads. */ }
        });
        return value.or(() -> Optional.ofNullable(defaultValue));
    }

    private String cacheKey(String key) { return "bakabooru:setting:" + key; }

    public String getValue(String key, String defaultValue) { return getOptionalSetting(key, defaultValue).orElse(defaultValue); }

    public String getActiveTaggerId() { return getValue(ACTIVE_TAGGER, null); }

    public String getActiveClipId() { return getValue(ACTIVE_CLIP, null); }

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

    public int getAnalysisMaxAttempts() {
        return getIntSetting(ANALYSIS_MAX_ATTEMPTS);
    }

    public long getAnalysisRetryBaseDelaySeconds() {
        return getLongSetting(ANALYSIS_RETRY_BASE_DELAY_SECONDS);
    }

    public long getAnalysisRetryMaxDelaySeconds() {
        return getLongSetting(ANALYSIS_RETRY_MAX_DELAY_SECONDS);
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
        int maxAttempts = parseInt(settings, ANALYSIS_MAX_ATTEMPTS);
        long retryBaseDelay = parseLong(settings, ANALYSIS_RETRY_BASE_DELAY_SECONDS);
        long retryMaxDelay = parseLong(settings, ANALYSIS_RETRY_MAX_DELAY_SECONDS);
        long retentionDays = parseLong(settings, UPLOAD_COMPLETED_RETENTION_DAYS);
        int inferenceConcurrency = parseInt(settings, INFERENCE_CONCURRENCY);

        requireRange(TAG_THRESHOLD, threshold, 0, 1);
        requireRange(ANALYSIS_MAX_ATTEMPTS, maxAttempts, 1, 20);
        requireRange(ANALYSIS_RETRY_BASE_DELAY_SECONDS, retryBaseDelay, 1, 3600);
        requireRange(UPLOAD_COMPLETED_RETENTION_DAYS, retentionDays, 1, 365);
        requireRange(INFERENCE_CONCURRENCY, inferenceConcurrency, 1, 64);
        if (retryMaxDelay < retryBaseDelay) {
            throw new IllegalArgumentException(ANALYSIS_RETRY_MAX_DELAY_SECONDS
                    + " must be greater than or equal to " + ANALYSIS_RETRY_BASE_DELAY_SECONDS);
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

    /** 批量更新已经存在的内部配置。 */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>(newSettings.keySet());

        List<Setting> existingSettings = settingRepository.findAllById(keys);

        if (existingSettings.size() != keys.size()) {
            Set<String> existingKeys = existingSettings.stream()
                    .map(Setting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        for (Setting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        settingRepository.saveAll(existingSettings);
        Runnable evict = () -> keys.forEach(key -> {
            try { redisTemplate.delete(cacheKey(key)); }
            catch (RuntimeException ignored) { /* A later cache miss will read the committed database value. */ }
        });
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { evict.run(); }
                    });
        } else {
            evict.run();
        }
    }
}
