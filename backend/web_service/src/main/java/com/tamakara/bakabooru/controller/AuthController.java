package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.system.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "系统", description = "系统认证")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "initialized", authService.isInitialized(),
                "passwordSet", authService.isPasswordSet()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.getOrDefault("password", ""));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setup(@RequestBody Map<String, String> body) {
        if (authService.isInitialized()) {
            return ResponseEntity.badRequest().build();
        }
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestBody Map<String, String> body) {
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }
}
