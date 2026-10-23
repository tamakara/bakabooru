package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Data
public class ImageDto {
    private Long id;
    private String title;
    private String fileName;
    private String extension;
    private Long size;
    private Integer width;
    private Integer height;
    private String hash;
    private String status;
    private Long viewCount;
    private String analysisStage;
    private String analysisError;
    private Instant analysisStartedAt;
    private Instant analysisCompletedAt;
    private List<ImageVectorDto> indexVectors;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ImageTagDto> tags;
    private String imageUrl;
    private String thumbnailUrl;
}

