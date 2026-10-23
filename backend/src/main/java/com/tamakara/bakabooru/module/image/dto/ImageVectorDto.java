package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ImageVectorDto {
    private String modelId;
    private String modelRevision;
    private String status;
    private String errorMessage;
    private Instant computedAt;
}
