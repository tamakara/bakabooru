package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缁崵绮虹拋銈堢槈閹貉冨煑閸?
 * 婢跺嫮鎮婇惂璇茬秿閵嗕胶濮搁幀浣诡梾閺屻儱寮风€靛棛鐖滅粻锛勬倞
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "API")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    @Operation(summary = "operation")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "initialized", authService.isInitialized(),
                "passwordSet", authService.isPasswordSet()
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "operation")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.getOrDefault("password", ""));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/setup")
    @Operation(summary = "operation")
    public ResponseEntity<Void> setup(@RequestBody Map<String, String> body) {
        if (authService.isInitialized()) {
            return ResponseEntity.badRequest().build();
        }
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    @Operation(summary = "operation")
    public ResponseEntity<Void> updatePassword(@RequestBody Map<String, String> body) {
        authService.setPassword(body.getOrDefault("password", ""));
        return ResponseEntity.ok().build();
    }
}
