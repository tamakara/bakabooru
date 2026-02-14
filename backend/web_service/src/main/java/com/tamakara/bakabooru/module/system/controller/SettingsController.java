package com.tamakara.bakabooru.module.system.controller;

import com.tamakara.bakabooru.module.storage.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
@Tag(name = "系统", description = "系统设置")
public class SettingsController {

    private final SystemSettingService systemSettingService;
    private final StorageService storageService;

    @GetMapping
    @Operation(summary = "获取所有设置")
    public Map<String, String> getAllSettings() {
        return systemSettingService.getAllSettings();
    }

    @PostMapping
    @Operation(summary = "更新设置")
    public void updateSettings(@RequestBody Map<String, String> settings) {
        systemSettingService.updateSettings(settings);
    }

    @PostMapping("/clear-cache")
    @Operation(summary = "清空缓存")
    public void clearCache() {
        storageService.clearCache();
    }
}
