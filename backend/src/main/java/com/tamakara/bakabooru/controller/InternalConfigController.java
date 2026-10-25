package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.config.InternalConfigProperties;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/config")
@RequiredArgsConstructor
public class InternalConfigController {
    private static final Set<String> ALLOWED_KEYS = Set.of(
            SystemSettingService.INFERENCE_CONCURRENCY,
            SystemSettingService.TAG_THRESHOLD,
            SystemSettingService.ANALYSIS_MAX_ATTEMPTS,
            SystemSettingService.ANALYSIS_RETRY_BASE_DELAY_SECONDS,
            SystemSettingService.ANALYSIS_RETRY_MAX_DELAY_SECONDS,
            SystemSettingService.ACTIVE_TAGGER,
            SystemSettingService.ACTIVE_CLIP
    );
    private final SystemSettingService settingService;
    private final InternalConfigProperties properties;

    @GetMapping("/{key}")
    public Map<String, String> get(@PathVariable String key,
                                   @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(properties.getInternalToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal token");
        }
        if (!ALLOWED_KEYS.contains(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown config key");
        return Map.of("key", key, "value", settingService.getValue(key, ""));
    }
}
