package com.tamakara.bakabooru.module.gallery.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchRequestDto {
    private String tags;
    private String keyword;
    private String semanticQuery;  // 鐠囶厺绠熼幓蹇氬牚閹兼粎鍌?
    private String status;
    private String randomSeed;
    private Integer widthMin;
    private Integer widthMax;
    private Integer heightMin;
    private Integer heightMax;
    private Long sizeMin;
    private Long sizeMax;
    private Integer page;
    private Integer size;
    private String sort;
    private List<String> vectorModelIds;
    private String vectorModelId;
}
