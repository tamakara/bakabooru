package com.tamakara.bakabooru.module.analysis.dto;

import java.util.List;

public record BatchVectorsRequest(List<Long> ids, List<String> modelIds) {
}
