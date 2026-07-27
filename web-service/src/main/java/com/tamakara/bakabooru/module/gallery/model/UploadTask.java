package com.tamakara.bakabooru.module.gallery.model;

import lombok.Data;

@Data
public class UploadTask {
    private String id;
    private String filename;
    private Long size;
    private String tempFilePath;
    private String errorMessage;
}

