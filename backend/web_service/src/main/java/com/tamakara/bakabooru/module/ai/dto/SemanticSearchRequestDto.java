package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SemanticSearchRequestDto {
    private String query;
    @JsonProperty("llm_url")
    private String llmUrl;
    @JsonProperty("llm_model")
    private String llmModel;
    @JsonProperty("llm_api_key")
    private String llmApiKey;
}
