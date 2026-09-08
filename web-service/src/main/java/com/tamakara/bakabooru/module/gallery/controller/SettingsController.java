package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.system.dto.SettingDefinitionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Map;
import java.util.List;

/**
 * 缁崵绮虹拋鍓х枂閹貉冨煑閸?
 * 缁狅紕鎮婇崗銊ョ湰闁板秶鐤嗛崣鍌涙殶
 */
@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
@Tag(name = "API")
public class SettingsController {

    private final SystemSettingService systemSettingService;
    private final AiServiceClient aiServiceClient;

    @GetMapping
    @Operation(summary = "operation")
    public Map<String, String> getAllSettings() {
        return systemSettingService.getEditableSettings();
    }

    @GetMapping("/metadata")
    public List<SettingDefinitionDto> getMetadata() {
        return systemSettingService.getDefinitionsWithCurrentValues();
    }

    @PostMapping
    @Operation(summary = "operation")
    public void updateSettings(@RequestBody Map<String, String> settings) {
        try {
            systemSettingService.updateEditableSettings(settings);
            try {
                aiServiceClient.updateRuntimeSettings(Map.of(
                        "device_mode", settings.getOrDefault("ai.device-mode", systemSettingService.getOptionalSetting("ai.device-mode", "auto")),
                        "cache_dir", settings.getOrDefault("ai.model-cache-dir", systemSettingService.getOptionalSetting("ai.model-cache-dir", "/model_cache")),
                        "inference_concurrency", Integer.parseInt(settings.getOrDefault("ai.inference-concurrency", systemSettingService.getOptionalSetting("ai.inference-concurrency", "1")))));
            } catch (RuntimeException ignored) {
                // Settings remain persisted while AI Service is offline; it receives them on its next reload.
            }
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }
}
