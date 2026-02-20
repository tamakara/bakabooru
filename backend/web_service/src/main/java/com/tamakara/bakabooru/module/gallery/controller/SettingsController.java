package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置控制器
 * 管理全局配置参数
 */
@RestController
@RequestMapping("/api/system/settings")
@RequiredArgsConstructor
@Tag(name = "系统设置", description = "全局参数配置")
public class SettingsController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @Operation(summary = "获取设置")
    public Map<String, String> getAllSettings() {
        return systemSettingService.getAllSettings();
    }

    @PostMapping
    @Operation(summary = "更新设置")
    public void updateSettings(@RequestBody Map<String, String> settings) {
        systemSettingService.updateSettings(settings);
    }
}
