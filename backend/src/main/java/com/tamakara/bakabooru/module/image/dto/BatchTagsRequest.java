package com.tamakara.bakabooru.module.image.dto;

import java.util.List;

public record BatchTagsRequest(List<Long> ids, String modelId) {
}
