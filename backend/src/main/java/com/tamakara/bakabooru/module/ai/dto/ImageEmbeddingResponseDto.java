package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ImageEmbeddingResponseDto {
    @JsonProperty("embedding")
    private List<Double> embedding;

}
