package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** CLIP 文本向量响应。 */
@Data
@NoArgsConstructor
public class EmbeddingResponseDto {
    private String text;
    private List<Double> embedding;
}
