package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CLIP Embedding 閸濆秴绨?DTO
 */
@Data
@NoArgsConstructor
public class EmbeddingResponseDto {
    private String text;
    private List<Double> embedding;
}
