package com.tamakara.bakabooru.module.image.dto;

import java.util.List;

public record BatchAiTagsRequest(List<Long> ids, String modelId) {
}
