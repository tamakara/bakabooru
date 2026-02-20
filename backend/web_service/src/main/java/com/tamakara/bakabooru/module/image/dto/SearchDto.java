package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

@Data
public class SearchDto {
    private String keyword;
    private String randomSeed;
    private Pageable pageable;
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
