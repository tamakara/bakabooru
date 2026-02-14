package com.tamakara.bakabooru.module.upload.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UploadTask {
    private String id;
    private String filename;
    private long size;
    private String errorMessage;
}

