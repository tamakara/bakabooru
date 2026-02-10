package com.tamakara.bakabooru.module.upload.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadTask {
    private String id;
    private String filename;
    private long size;
    private UploadStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 内部使用
    private String hash;
    private boolean enableTagging;

    public enum UploadStatus {
        PENDING, // 等待处理
        UPLOADING, // 如果我们一次性接收文件，则实际上不使用，但如果我们流式传输或分块传输，则很有用
        PROCESSING, // 预处理（哈希，缩略图）
        TAGGING, // AI 标注器
        SAVING, // 保存到数据库
        COMPLETED,
        FAILED
    }
}

