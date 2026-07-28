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
@Tag(name = "上传", description = "图片上传操作")
public class UploadController {

    private final UploadJobService uploadJobService;

    @PostMapping
    @Operation(summary = "上传图片", description = "上传单个图片并创建持久化任务")
    public void createTask(@RequestParam("file") MultipartFile file) {
        uploadJobService.createTask(file);
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取任务列表信息")
    public TasksInfoDto getTasksInfo() {
        return uploadJobService.getTasksInfo();
    }

    @PostMapping("/tasks")
    @Operation(summary = "重试上传任务")
    public void retryTask(@RequestParam UUID id) {
        uploadJobService.retryTask(id);
    }

    @DeleteMapping("/tasks")
    @Operation(summary = "清空失败任务")
    public void clearFailedTasks() {
        uploadJobService.clearFailedTasks();
    }
}
