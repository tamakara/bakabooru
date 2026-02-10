package com.tamakara.bakabooru.module.gallery.dto;

import lombok.Data;

@Data
public class SearchRequestDto {
    private String tagSearch;
    private String keyword;
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
}
