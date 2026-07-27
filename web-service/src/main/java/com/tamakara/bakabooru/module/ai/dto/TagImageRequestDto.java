package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TagImageRequestDto {
    @JsonProperty("object_name")
    private String objectName;

    @JsonProperty("threshold")
    private Double threshold;
}
