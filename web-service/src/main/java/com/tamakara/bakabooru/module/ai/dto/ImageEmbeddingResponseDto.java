package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ImageEmbeddingResponseDto {
    private boolean success;

    @JsonProperty("embedding")
    private List<Double> embedding;

    private String error;
}
