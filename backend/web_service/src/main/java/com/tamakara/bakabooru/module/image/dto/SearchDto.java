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
    private String aiStatus;
    private Integer widthMin;
    private Integer widthMax;
    private Integer heightMin;
    private Integer heightMax;
    private Long sizeMin;
    private Long sizeMax;
    private Double distanceThreshold;
    private Set<String> positiveTags;
    private Set<String> negativeTags;
    private List<Float> embedding;  // CLIP 向量用于语义搜索
}
