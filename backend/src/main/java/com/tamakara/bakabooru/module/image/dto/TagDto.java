package com.tamakara.bakabooru.module.image.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagDto {
    private Long id;
    private String name;
    private String type;
}

