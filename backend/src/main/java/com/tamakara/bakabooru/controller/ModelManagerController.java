package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.model.dto.ModelDto;
import com.tamakara.bakabooru.module.model.service.ModelManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/models")
@RequiredArgsConstructor
public class ModelManagerController {
    private final ModelManagerService service;

    @GetMapping
    public List<ModelDto> list() {
        return service.list();
    }

    @PostMapping("/{id}/download")
    public ModelDto download(@PathVariable String id) {
        return service.markDownloadStarted(id);
    }

    @PostMapping("/{id}/enable")
    public ModelDto enable(@PathVariable String id) {
        return service.enable(id);
    }
}
