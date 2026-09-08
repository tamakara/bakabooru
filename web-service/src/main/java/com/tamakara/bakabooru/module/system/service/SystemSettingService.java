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
            "ai.inference-concurrency",
            "ai.device-mode",
            "ai.model-cache-dir"
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
                new SettingDefinitionDto("ai.device-mode", "Device mode", "text", "auto", "RELOAD", false, "Inference device such as auto, cpu, or cuda"),
                new SettingDefinitionDto("ai.model-cache-dir", "Model cache directory", "text", "/model_cache", "RELOAD", false, "AI model artifact cache directory"),
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

    /**
     * 闂傚倷绀侀崥瀣磿閹惰棄搴婇柤鑹扮堪娴滃綊鏌涢妷顔煎缂佺姰鍎甸弻宥堫檨闁告挾鍠庨锝夊垂椤愩垻绐為梺褰掑亰閸撴瑧鎸х€ｎ剛纾介柛灞捐壘閺嬨倝鏌涢悩铏磳闁糕斁鍋?     */
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

    // --- 缂傚倸鍊风欢锟犲磻婢舵劦鏁嬬憸鏃堝箖濡ゅ懏鐓ラ悗锝庡亜椤€愁渻閵堝棗绗傜紒鈧担瑙勫劅濠电姴娲ょ痪褔鏌涢锝囩畺闁革絿鎳撻埞鎴︻敋閸涱剟鍋楅悗娈垮櫘閸撴盯骞夐幘顔肩妞ゆ挻澹曢崑?---

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
     * 闂傚倷绀侀幉锟犮€冮崱妞曞搫螣娓氼垳鍔峰銈嗙墱閸嬫盯宕￠幎鑺ュ€垫繛鎴烆伆閹达附鍋傞柍?
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
        String deviceMode = requireValue(settings, "ai.device-mode").toLowerCase(Locale.ROOT);
        if (!Set.of("auto", "cpu", "cuda").contains(deviceMode)) {
            throw new IllegalArgumentException("ai.device-mode must be auto, cpu, or cuda");
        }
        requireValue(settings, "ai.service-url");
        requireValue(settings, "ai.model-cache-dir");
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
     * 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堝础閹惰姤鍊垫繛鎴烆伆閹达附鍋傞柍?(濠电姷鏁搁崕鎴犲緤閽樺鏆︽い鎺戝€甸崑鎾舵兜閸涱喚褰ч柣搴ｆ暩閸樠嗙亽闂佸憡绻傜€氱兘宕靛▎鎾粹拺?
     */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = new HashSet<>(newSettings.keySet());

        // 1. 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７濠电偛妯婃禍婊堟嫅閻斿吋鐓忓鑸殿焽閸樻盯鏌?DB (1濠?SQL: SELECT * FROM table WHERE id IN (...))
        List<SystemSetting> existingSettings = systemSettingRepository.findAllById(keys);

        // 2. 婵犵數鍋為崹鍫曞箰閸洖纾块柡灞诲劜閸嬪绱掔€ｎ収鍤︽繛鎴欏灩閻掑灚銇勯幒鎴濐仼闁藉啰鍠栭弻鏇熷緞濡櫣浠紓浣瑰姈椤ㄥ牓鎯€椤忓牜鏁囩憸宥夊几濞嗘垹纾肩紓浣姑崫鐑橆殽閻愯尙效闁糕斁鍋撳銈嗗笒鐎氼剛鈧?Key 闂傚倸鍊风欢锟犲窗濞戞娑㈠礋椤栨稑浠遍梺闈浥堥弲娑㈠箲閸洘鐓忓┑鐐茬仢閳ь剚顨婇幊婵囥偅閸愨晝鍘告繛杈剧到閹碱偊銆傞懖鈹惧亾鐟欏嫭灏柣鎺炵畵楠炲棝寮崼婵堫啋闂佽壈澹堝▔娑㈢嵁閳?
        if (existingSettings.size() != keys.size()) {
            // 闂傚倷鑳堕幊鎾绘倶濮樿泛纾块柟鎯版閸氳銇勯幘鍗炵仼濞磋偐濮甸妵鍕疀閹捐櫕娈婚梺?Key 婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柟缁㈠枟閸嬨倝鏌曟繛鐐珔闁?(闂傚倷绀侀幉锟犳偡椤栫偛鍨傞柛顐ｆ礀閻掑灚銇勯幒鍡椾壕濡炪伇鈧崑鎾剁磽娴ｄ粙鍝虹紒璇茬墦楠炲啯绂掔€ｎ€晠鏌曢崼婵囩┛濠㈣锕㈠铏规兜閸涱垰鐗氶梺绋挎捣閺佽顕ｉ锝囩瘈闁搞儜鍜佸斀闂備礁缍婇崑濠囧窗閹惧顩?
            Set<String> existingKeys = existingSettings.stream()
                    .map(SystemSetting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        // 3. 闂傚倷绶氬鑽ゆ嫻閻旂厧绀夌€光偓閸曨偉鍩為柣搴ｆ暩绾爼宕戦幘鏂ユ婵炲拋鍘奸妶鎼佸箠濞嗘挸绠ｉ柨鏇楀亾闁告濞婇幃妤€鈽夊▍铏灴閹繝鍩€?Entity 闂備浇顕уù鐑藉极閹间降鈧焦绻濋崶銊ョ樁?
        for (SystemSetting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        // 4. 闂傚倷绀佺紞濠傤焽瑜忕槐鐐寸節閸パ囨７闂佺粯鍨堕…鍥煘瀹ュ棛绡€闂傚牊绋掗ˉ鐘绘煙閻ｅ苯鈻堥柡?DB (1濠?SQL 婵犵數鍋涢悺銊╁吹鎼淬劌纾归柟鐐劶婵啿霉閻撳海鎽犻柡鍜佸墴閹﹢鎮欓懜娈挎缂備焦褰冨锟犲蓟濞戙垹绠涙い鎺嗗亾閻㈩垳濮风槐?JPA 闂備浇顕ф绋匡耿闁秴纾婚柣鏃囧亹瀹撲線鏌涢妷顔煎闁哄拋鍓涢埀顒€鍘滈崑鎾绘煕閺囨ê濡煎ù婊呭亾閹便劌顫滈崱妤€顫梺绯曟櫆椤ㄥ﹪寮?batch update)
        systemSettingRepository.saveAll(existingSettings);

    }
}
