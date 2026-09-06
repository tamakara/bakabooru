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
    public static final String AI_DEFAULT_VECTOR_MODELS = "ai.default-vector-models";
    public static final String BOOTSTRAP_DATABASE_URL = "bootstrap.database-url";
    public static final String BOOTSTRAP_STORAGE_ENDPOINT = "bootstrap.storage-endpoint";

    private static final Set<String> EDITABLE_KEYS = Set.of(
                new SettingDefinitionDto(TAG_THRESHOLD, "Tag threshold", "number", "0.61", "HOT", false, "Minimum AI tag confidence"),
            AI_MAX_ATTEMPTS,
            AI_RETRY_BASE_DELAY_SECONDS,
                new SettingDefinitionDto(AI_RETRY_MAX_DELAY_SECONDS, "AI retry max delay", "integer", "1800", "HOT", false, "Maximum retry delay seconds"),
            UPLOAD_COMPLETED_RETENTION_DAYS,
            AI_DEFAULT_VECTOR_MODELS
    );

    private final SystemSettingRepository systemSettingRepository;

    public List<SettingDefinitionDto> getDefinitions() {
        return List.of(
                new SettingDefinitionDto(TAG_THRESHOLD, "Tag threshold", "number", "0.61", "HOT", false, "Minimum AI tag confidence"),
                new SettingDefinitionDto(AI_MAX_ATTEMPTS, "AI 闂備焦褰冪粔鐑芥儊椤栨埃鏋庨柍鈺佸暞濞?", "integer", "5", "HOT", false, "闂佸憡顨嗗ú鐔煎Υ?AI 婵炲濮鹃褎鎱ㄩ悢鐓庡珘闁逞屽墯瀵板嫯顦归柛锝呮憸閹风娀寮撮悤浣镐还闂?)",
                new SettingDefinitionDto(AI_RETRY_BASE_DELAY_SECONDS, "AI 闂備焦褰冪粔鐑芥儊椤栫偛绀嗘繝闈涙－濞兼鈧偣鍊栭崕鑲╂崲?", "integer", "30", "HOT", false, "缂?)",
                new SettingDefinitionDto(AI_RETRY_MAX_DELAY_SECONDS, "AI retry max delay", "integer", "1800", "HOT", false, "Maximum retry delay seconds"),
                new SettingDefinitionDto(UPLOAD_COMPLETED_RETENTION_DAYS, "婵炴垶鎸搁敃锝囨閼哥數顩烽悹鍥ㄥ絻椤倕菐閸ャ劎绠橀柡鍡忓亾闂佸搫鍟悥鐓幬?", "integer", "7", "HOT", false, "婵?)",
                new SettingDefinitionDto(AI_DEFAULT_VECTOR_MODELS, "婵帗绋掗…鍫ヮ敇閼姐倖顫曢柕蹇曞Х缁屽潡鏌涘顓炵伌闁革絽鎼灒闁炽儱纾埀?", "text", "clip-vit-base-patch32", "HOT", false, "闂備緡鍋呴〃鍛般亹閸ф绀嗛柛鈩冪⊕椤撻箖鏌ｉ妸銉ヮ仾閼垛晠鏌?ID"),
                new SettingDefinitionDto(BOOTSTRAP_DATABASE_URL, "闂佽桨鑳舵晶妤€鐣垫担瑙勫劅闁规儳婀辩粻楣冩煙?", "text", "jdbc:postgresql://postgres:5432/bakabooru", "BOOTSTRAP", false, "闂佸憡鍑归崹鐗堟叏閳哄啰妫憸鏃堝储閵堝洨纾炬い鏇炴缁€澶愭煕濞嗘ü娴锋い?)",
                new SettingDefinitionDto(BOOTSTRAP_STORAGE_ENDPOINT, "Storage endpoint", "text", "http://minio:9000", "BOOTSTRAP", false, "Storage service endpoint"),
        );
    }

    /**
     * 闂佸吋鍎抽崲鑼躲亹閸ヮ剙绠ラ柍褜鍓熷鍨緞閹邦剙璧嬬紓鍌氬枤閸犳捇鍩€?     */
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

    // --- 缂備緡鍋夐褔鎮楅柨瀣妞ゆ帊绀佹惔濠囧级閸繃鍣瑰┑顕呬邯瀵剛鎲撮崟顓溾偓?---

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
     * 闂佸憡顨嗗ú妯侯焽椤栫偛鍗抽悗娑櫳戦悡鈧?
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
        String defaultModels = settings.get(AI_DEFAULT_VECTOR_MODELS);

                new SettingDefinitionDto(TAG_THRESHOLD, "Tag threshold", "number", "0.61", "HOT", false, "Minimum AI tag confidence"),
        requireRange(AI_MAX_ATTEMPTS, maxAttempts, 1, 20);
        requireRange(AI_RETRY_BASE_DELAY_SECONDS, retryBaseDelay, 1, 3600);
                new SettingDefinitionDto(AI_RETRY_MAX_DELAY_SECONDS, "AI retry max delay", "integer", "1800", "HOT", false, "Maximum retry delay seconds"),
        requireRange(UPLOAD_COMPLETED_RETENTION_DAYS, retentionDays, 1, 365);
        if (retryMaxDelay < retryBaseDelay) {
            throw new IllegalArgumentException(AI_RETRY_MAX_DELAY_SECONDS
                    + " must be greater than or equal to " + AI_RETRY_BASE_DELAY_SECONDS);
        }
        if (defaultModels != null && defaultModels.isBlank()) {
            throw new IllegalArgumentException(AI_DEFAULT_VECTOR_MODELS + " is required");
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
     * 闂佸綊娼х紞濠囧闯濞差亜鍗抽悗娑櫳戦悡鈧?(濠电儑绲藉畷顒傗偓纭呮珪鐎电厧螣閸濆嫷鍤欓梺?
     */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>(newSettings.keySet());

        // 1. 闂佸綊娼х紞濠囧闯濞差亜钃熼柕澶樼厛閸?DB (1濠?SQL: SELECT * FROM table WHERE id IN (...))
        List<SystemSetting> existingSettings = systemSettingRepository.findAllById(keys);

        // 2. 婵炴垶鎸堕崕鏌ユ偋缁嬭娑㈠焵椤掑嫬钃熼柕澶樺灣缁愭鐥褍鏋欑紒缁樺哺楠炲秹鍩€椤掑嫬瀚?Key 闂備緡鍠涘Λ鍕偤閵娾晛鎹堕柕濞垮€楅懝楣冩煛娴ｅ搫顣肩€规挷鐒﹂幆鏃堝箻閼艰泛骞€
        if (existingSettings.size() != keys.size()) {
            // 闂佺懓鐏氶崕鎶藉吹椤撱垹浼犳い蹇撳暣閸?Key 婵炴垶鎸哥粔鎾偤閵娾晛鎹?(闂佸憡鐟崹鍫曞焵椤掆偓椤р偓缂佽鲸绻堥幃浠嬪Ω閵堝洩澹橀梺纭呯堪閸庣敻寮繝鍥х闁归偊鍠撴禒?
            Set<String> existingKeys = existingSettings.stream()
                    .map(SystemSetting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        // 3. 闂侀潻璐熼崝宀勫船鐎电硶鍋撳☉娅ジ鎳欓幋锕€鍗抽悗娑櫳戦悡鈧?Entity 闁诲海鏁搁、濠囨寘?
        for (SystemSetting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        // 4. 闂佸綊娼х紞濠囧闯閻戞鈹嶆繝闈涙閹界娀鏌?DB (1濠?SQL 婵炲瓨鍤庨崐鎾惰姳娴煎瓨鏅悘鐐舵缁插潡鏌涢幇顒€甯犵紒?JPA 闁诲骸婀遍崑鐔肩嵁閸ヮ剚鏅€光偓閸曘劌浜炬慨姗嗗墰閸╂鏌?batch update)
        systemSettingRepository.saveAll(existingSettings);

    }
}
