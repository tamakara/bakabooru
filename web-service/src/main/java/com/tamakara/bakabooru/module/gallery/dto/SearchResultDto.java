package com.tamakara.bakabooru.module.gallery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResultDto<T> {
    private List<T> content;
    private Integer page;
    private Integer size;
    private Boolean hasNext;
}
