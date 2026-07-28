package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyzeImageRequestDto {
    @JsonProperty("object_name")
    private String objectName;
    private double threshold;
}
