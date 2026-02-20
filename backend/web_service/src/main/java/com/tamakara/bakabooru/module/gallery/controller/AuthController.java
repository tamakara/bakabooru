package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统认证控制器
 * 处理登录、状态检查及密码管理
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "系统认证", description = "认证与权限管理")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    @Operation(summary = "获取状态", description = "检查系统初始化状态")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "initialized", authService.isInitialized(),
                "passwordSet", authService.isPasswordSet()
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登���")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.getOrDefault("password", ""));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/setup")
    @Operation(summary = "系统初始化", description = "首次设置密码")
    public ResponseEntity<Void> setup(@RequestBody Map<String, String> body) {
        if (authService.isInitialized()) {
            return ResponseEntity.badRequest().build();
        }
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码")
    public ResponseEntity<Void> updatePassword(@RequestBody Map<String, String> body) {
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }
}
