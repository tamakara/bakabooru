package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SearchDto {
    private String keyword;
    private String randomSeed;
    private Integer page = 0;
    private Integer size = 20;
    private String sortProperty = "createdAt";
    private String sortDirection = "DESC";
    private String status;
    private Integer widthMin;
    private Integer widthMax;
    private Integer heightMin;
    private Integer heightMax;
    private Long sizeMin;
    private Long sizeMax;
    private Double distanceThreshold;
    private Set<String> positiveTags;
    private Set<String> negativeTags;
    private List<Float> embedding;  // CLIP 閸氭垿鍣洪悽銊ょ艾鐠囶厺绠熼幖婊呭偍
    private List<String> vectorModelIds;
    private String vectorModelId;
}
