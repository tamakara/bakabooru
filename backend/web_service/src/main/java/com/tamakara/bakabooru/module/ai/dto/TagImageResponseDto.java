package com.tamakara.bakabooru.module.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TagImageResponseDto {
    private boolean success;
    private Map<String,Double> data;
    private String error;
}
