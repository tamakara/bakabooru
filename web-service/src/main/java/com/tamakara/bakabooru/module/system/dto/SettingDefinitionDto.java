package com.tamakara.bakabooru.module.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettingDefinitionDto {
    private String key;
    private String label;
    private String type;
    private String defaultValue;
    private String scope;
    private boolean secret;
    private String description;
}
