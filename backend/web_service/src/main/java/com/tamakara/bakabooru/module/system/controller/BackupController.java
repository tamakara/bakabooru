package com.tamakara.bakabooru.module.system.controller;

import com.tamakara.bakabooru.module.system.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/system/backup")
@RequiredArgsConstructor
@Tag(name = "系统", description = "系统维护")
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    @Operation(summary = "下载备份", description = "创建并下载完整系统备份")
    public ResponseEntity<Resource> downloadBackup() throws IOException {
        File backupFile = backupService.createBackup();
        Resource resource = new FileSystemResource(backupFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backupFile.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/restore")
    @Operation(summary = "还原备份", description = "从备份文件还原系统")
    public void restoreBackup(@RequestParam("file") MultipartFile file) throws IOException {
        backupService.restoreBackup(file);
    }

    @DeleteMapping("/reset")
    @Operation(summary = "重置系统", description = "删除所有数据并重置设置")
    public void resetSystem() throws IOException {
        backupService.resetSystem();
    }
}
