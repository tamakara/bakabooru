package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.setting.service.SettingService;
import com.tamakara.bakabooru.module.setting.dto.SettingDefinitionDto;
import com.tamakara.bakabooru.module.setting.dto.SettingsUpdateResponse;
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

    private final SettingService settingService;

    @GetMapping
    @Operation(summary = "operation")
    public Map<String, String> getAllSettings() {
        return settingService.getEditableSettings();
    }

    @GetMapping("/metadata")
    public List<SettingDefinitionDto> getMetadata() {
        return settingService.getDefinitionsWithCurrentValues();
    }

    @PostMapping
    @Operation(summary = "operation")
    public SettingsUpdateResponse updateSettings(@RequestBody Map<String, String> settings) {
        try {
            settingService.updateEditableSettings(settings);
            return new SettingsUpdateResponse(true, false, List.of(), "配置已保存，运行服务将按周期同步");
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }
}
