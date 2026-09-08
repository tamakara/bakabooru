package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SemanticSearchRequestDto {
    private String query;
    @com.fasterxml.jackson.annotation.JsonProperty("model_id")
    private String modelId;
}
