package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SemanticSearchResponseDto {
    private boolean success;
    private TagsResult tags;
    @JsonProperty("clip_search")
    private ClipSearchResult clipSearch;
    private String error;

    @Data
    @NoArgsConstructor
    public static class TagsResult {
        private List<String> positive;
        private List<String> negative;
    }

    @Data
    @NoArgsConstructor
    public static class ClipSearchResult {
        private String text;
        private List<Double> embedding;
    }
}
