package com.tamakara.bakabooru.module.image.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageTagDto {
    private Long id;
    private String name;
    private String type;
    private Double score;
}