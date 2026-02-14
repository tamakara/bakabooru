package com.tamakara.bakabooru.module.upload.controller;

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
    @Operation(summary = "上传文件", description = "上传单个文件并开始处理")
    public UploadTask uploadFile(@RequestParam("file") MultipartFile file) {
        return uploadTaskService.createTask(file);
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取上传任务列表", description = "获取所有上传任务的状态")
    public List<UploadTask> listTasks() {
        return uploadTaskService.getAllTasks();
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "获取上传任务", description = "获取特定上传任务的状态")
    public UploadTask getTask(@PathVariable String id) {
        return uploadTaskService.getTask(id);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除上传任务", description = "删除特定上传任务")
    public void deleteTask(@PathVariable String id) {
        uploadTaskService.deleteTask(id);
    }

    @DeleteMapping("/tasks")
    @Operation(summary = "清空上传任务", description = "清空所有上传任务")
    public void clearTasks() {
        uploadTaskService.clearTasks();
    }
}
