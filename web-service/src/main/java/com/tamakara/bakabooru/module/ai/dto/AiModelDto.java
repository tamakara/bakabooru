package com.tamakara.bakabooru.module.ai.dto;

import com.tamakara.bakabooru.module.ai.entity.AiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiModelDto {
    private String id;
    private String name;
    private String capability;
    private String type;
    private String version;
    private Integer dimension;
    private String status;
    private String artifactState;
    private java.util.List<String> capabilities;
    private String errorMessage;

    public static AiModelDto from(AiModel model) {
        AiModelDto dto = new AiModelDto();
        dto.id = model.getId();
        dto.name = model.getName();
        dto.capability = model.getCapability();
        dto.type = model.getModelType() == null ? ("TAGGING".equals(model.getCapability()) ? "TAGGER" : "CLIP") : model.getModelType();
        dto.version = model.getVersion();
        dto.dimension = model.getDimension();
        dto.status = model.getStatus();
        dto.artifactState = "READY".equals(model.getStatus()) ? "READY" : "DOWNLOADING".equals(model.getStatus()) ? "DOWNLOADING" : "FAILED".equals(model.getStatus()) ? "FAILED" : "NOT_INSTALLED";
        dto.capabilities = "TAGGER".equals(dto.type)
                ? java.util.List.of("TAGS")
                : java.util.List.of("IMAGE_EMBEDDING", "TEXT_EMBEDDING");
        return dto;
    }

    public static AiModelDto fromRemote(java.util.Map<?, ?> model) {
        AiModelDto dto = new AiModelDto();
        dto.id = value(model, "id");
        dto.name = value(model, "name");
        dto.type = value(model, "type");
        dto.version = value(model, "version");
        Object dimension = model.get("dimension");
        dto.dimension = dimension instanceof Number n ? n.intValue() : null;
        dto.artifactState = value(model, "artifactState");
        Object capabilities = model.get("capabilities");
        if (capabilities instanceof java.util.List<?> list) {
            dto.capabilities = list.stream().map(String::valueOf).toList();
        } else {
            dto.capabilities = java.util.List.of();
        }
        dto.errorMessage = value(model, "errorMessage");
        return dto;
    }

    private static String value(java.util.Map<?, ?> model, String key) {
        Object value = model.get(key);
        return value == null ? null : value.toString();
    }
}
