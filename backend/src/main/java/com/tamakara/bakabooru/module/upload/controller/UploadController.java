package com.tamakara.bakabooru.module.upload.controller;

import com.tamakara.bakabooru.module.upload.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.upload.service.UploadJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "API")
public class UploadController {

    private final UploadJobService uploadJobService;

    @PostMapping
    @Operation(summary = "operation")
    public void createTask(@RequestParam("file") MultipartFile file,
                           @RequestParam(required = false) String tagModelId,
                           @RequestParam(required = false) String vectorModelIds) {
        uploadJobService.createTask(file, tagModelId, vectorModelIds);
    }

    @GetMapping("/tasks")
    @Operation(summary = "operation")
    public TasksInfoDto getTasksInfo() {
        return uploadJobService.getTasksInfo();
    }

    @PostMapping("/tasks")
    @Operation(summary = "operation")
    public void retryTask(@RequestParam UUID id) {
        uploadJobService.retryTask(id);
    }

    @DeleteMapping("/tasks")
    @Operation(summary = "operation")
    public void clearFailedTasks() {
        uploadJobService.clearFailedTasks();
    }
}
