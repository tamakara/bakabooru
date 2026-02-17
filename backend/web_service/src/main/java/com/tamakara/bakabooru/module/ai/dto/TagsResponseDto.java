package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标签提取响应 DTO
 */
@Data
@NoArgsConstructor
public class TagsResponseDto {
    private boolean success;
    private List<String> positive;
    private List<String> negative;
    private String error;
}
