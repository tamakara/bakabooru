package com.tamakara.bakabooru.module.analysis.dto;

import java.util.List;

public record BatchTagsRequest(List<Long> ids, String modelId) {
}
