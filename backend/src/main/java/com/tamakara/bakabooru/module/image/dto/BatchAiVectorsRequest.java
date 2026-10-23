package com.tamakara.bakabooru.module.image.dto;

import java.util.List;

public record BatchAiVectorsRequest(List<Long> ids, List<String> modelIds) {
}
