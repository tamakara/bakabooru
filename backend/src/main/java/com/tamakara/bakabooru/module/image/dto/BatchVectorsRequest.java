package com.tamakara.bakabooru.module.image.dto;

import java.util.List;

public record BatchVectorsRequest(List<Long> ids, List<String> modelIds) {
}
