package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/internal/config")
@RequiredArgsConstructor
public class InternalConfigController {
    private static final Set<String> ALLOWED_KEYS = Set.of(
            SettingService.INFERENCE_CONCURRENCY,
            SettingService.TAG_THRESHOLD,
            SettingService.ANALYSIS_MAX_ATTEMPTS,
            SettingService.ANALYSIS_RETRY_BASE_DELAY_SECONDS,
            SettingService.ANALYSIS_RETRY_MAX_DELAY_SECONDS,
            SettingService.UPLOAD_COMPLETED_RETENTION_DAYS,
            SettingService.ACTIVE_TAGGER,
            SettingService.ACTIVE_CLIP
    );
    private final SettingService settingService;

    @GetMapping("/{key}")
    public Map<String, Map<String, String>> get(@PathVariable String key) {
        validateKey(key);
        return valuesResponse(List.of(key));
    }

    @GetMapping
    public Map<String, Map<String, String>> getMany(
            @RequestParam(name = "keys", required = false) String keys) {
        if (keys == null || keys.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keys is required");
        }
        List<String> requested = Arrays.stream(keys.split(","))
                .map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
        if (requested.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keys is required");
        requested.forEach(this::validateKey);
        return valuesResponse(requested);
    }

    private Map<String, Map<String, String>> valuesResponse(List<String> keys) {
        Map<String, String> values = new LinkedHashMap<>();
        keys.forEach(key -> values.put(key, settingService.getValue(key, defaultValue(key))));
        return Map.of("values", values);
    }

    private String defaultValue(String key) {
        if (SettingService.TAG_THRESHOLD.equals(key)) return "0.61";
        if (SettingService.ANALYSIS_MAX_ATTEMPTS.equals(key)) return "5";
        if (SettingService.ANALYSIS_RETRY_BASE_DELAY_SECONDS.equals(key)) return "30";
        if (SettingService.ANALYSIS_RETRY_MAX_DELAY_SECONDS.equals(key)) return "1800";
        if (SettingService.UPLOAD_COMPLETED_RETENTION_DAYS.equals(key)) return "7";
        if (SettingService.INFERENCE_CONCURRENCY.equals(key)) return "1";
        return "";
    }

    private void validateKey(String key) {
        if (!ALLOWED_KEYS.contains(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown config key");
    }
}
