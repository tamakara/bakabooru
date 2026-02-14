package com.tamakara.bakabooru.module.upload.controller;

import com.tamakara.bakabooru.module.upload.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.upload.model.UploadTask;
import com.tamakara.bakabooru.module.upload.service.UploadTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "上传", description = "图片上传操作")
public class UploadController {

    private final UploadTaskService uploadTaskService;

    @PostMapping
    @Operation(summary = "上传图片", description = "上传单个图片并创建任务")
    public void uploadFile(@RequestParam("file") MultipartFile file) {
        uploadTaskService.createTask(file);
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取任务列表信息", description = "获取务列表信息的状态")
    public TasksInfoDto getTasksInfo() {
        return uploadTaskService.getTasksInfo();
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除上传任务", description = "删除特定上传任务")
    public void deleteTask(@PathVariable String id) {
        uploadTaskService.deleteTask(id);
    }

    @PostMapping
    @Operation(summary = "重试上传任务", description = "重试特定上传任务")
    public void retryTask(@RequestParam String id) {
        uploadTaskService.retryTask(id);
    }

    @DeleteMapping("/tasks")
    @Operation(summary = "清空上传任务", description = "清空所有上传任务")
    public void clearTasks() {
        uploadTaskService.clearTasks();
    }
}
