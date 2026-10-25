package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.model.client.ModelManagerClient;
import com.tamakara.bakabooru.module.system.dto.SettingDefinitionDto;
import com.tamakara.bakabooru.module.system.dto.SettingsUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Map;
import java.util.List;

/** 运行时系统设置接口。 */
@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
@Tag(name = "API")
public class SettingsController {

    private final SystemSettingService systemSettingService;
    private final ModelManagerClient modelManagerClient;

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
    public SettingsUpdateResponse updateSettings(@RequestBody Map<String, String> settings) {
        try {
            systemSettingService.updateEditableSettings(settings);
            try {
                modelManagerClient.updateRuntimeSettings(Map.of(
                        "inference_concurrency", Integer.parseInt(systemSettingService.getValue(SystemSettingService.INFERENCE_CONCURRENCY, "1"))));
            } catch (RuntimeException ignored) {
                // Settings remain persisted while Model Manager is offline; it receives them on its next reload.
            }
            return new SettingsUpdateResponse(true, false, List.of(), "配置已生效");
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }
}
