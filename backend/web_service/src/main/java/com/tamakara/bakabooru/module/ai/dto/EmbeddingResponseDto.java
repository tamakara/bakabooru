package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CLIP Embedding 响应 DTO
 */
@Data
@NoArgsConstructor
public class EmbeddingResponseDto {
    private boolean success;
    private String text;
    private List<Double> embedding;
    private String error;
}
