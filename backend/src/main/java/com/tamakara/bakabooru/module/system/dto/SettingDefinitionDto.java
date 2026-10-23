package com.tamakara.bakabooru.module.system.dto;

import lombok.Data;

@Data
public class SettingDefinitionDto {
    private String key;
    private String label;
    private String type;
    private String defaultValue;
    private String scope;
    private boolean secret;
    private String description;
    private String currentValue;
    private boolean requiresRestart;

    public SettingDefinitionDto(String key, String label, String type, String defaultValue,
                                String scope, boolean secret, String description) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.scope = scope;
        this.secret = secret;
        this.description = description;
    }
}
