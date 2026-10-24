package com.tamakara.bakabooru.module.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageAnalysisRequestDto {
    @JsonProperty("object_name")
    private String objectName;
    private double threshold;
    @JsonProperty("tag_model_id")
    private String tagModelId;
    @JsonProperty("vector_model_ids")
    private java.util.List<String> vectorModelIds;

    public ImageAnalysisRequestDto(String objectName, double threshold) {
        this(objectName, threshold, null, null);
    }
}
