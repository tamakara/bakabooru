package com.tamakara.bakabooru.module.ai.dto;

import com.tamakara.bakabooru.module.ai.entity.AiModel;
import lombok.Data;

@Data
public class AiModelDto {
    private String id;
    private String name;
    private String capability;
    private String version;
    private Integer dimension;
    private String status;
    private boolean downloaded;

    public static AiModelDto from(AiModel model) {
        AiModelDto dto = new AiModelDto();
        dto.id = model.getId();
        dto.name = model.getName();
        dto.capability = model.getCapability();
        dto.version = model.getVersion();
        dto.dimension = model.getDimension();
        dto.status = model.getStatus();
        dto.downloaded = "READY".equals(model.getStatus()) || "DISABLED".equals(model.getStatus());
        return dto;
    }
}
