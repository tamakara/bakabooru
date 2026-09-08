package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.ai.dto.AiModelDto;
import com.tamakara.bakabooru.module.ai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
public class AiModelController {
    private final AiModelService service;

    @GetMapping
    public List<AiModelDto> list() {
        return service.list();
    }

    @PostMapping("/{id}/download")
    public AiModelDto download(@PathVariable String id) {
        return service.markDownloadStarted(id);
    }

    @DeleteMapping("/{id}/artifact")
    public AiModelDto uninstall(@PathVariable String id) {
        return service.uninstall(id);
    }
}
