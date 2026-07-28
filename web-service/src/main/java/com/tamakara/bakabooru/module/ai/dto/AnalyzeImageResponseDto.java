package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AnalyzeImageResponseDto {
    private Map<String, Double> tags;
    private List<Double> embedding;
}
