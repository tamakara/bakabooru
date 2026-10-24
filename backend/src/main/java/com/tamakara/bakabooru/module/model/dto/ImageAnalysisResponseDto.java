package com.tamakara.bakabooru.module.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ImageAnalysisResponseDto {
    private Map<String, Double> tags;
    private List<Double> embedding;
    private Map<String, List<Double>> embeddings;
}
