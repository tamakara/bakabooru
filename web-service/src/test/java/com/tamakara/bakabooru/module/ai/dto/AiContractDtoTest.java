package com.tamakara.bakabooru.module.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiContractDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzeRequestUsesOpenApiFieldNames() throws Exception {
        String json = objectMapper.writeValueAsString(
                new AnalyzeImageRequestDto("original/hash", 0.61)
        );

        assertThat(json).contains("\"object_name\":\"original/hash\"");
        assertThat(json).contains("\"threshold\":0.61");
    }

    @Test
    void analyzeResponseMatchesOpenApiShape() throws Exception {
        String json = """
                {"tags":{"sample":0.9},"embedding":[0.1,0.2]}
                """;

        AnalyzeImageResponseDto response = objectMapper.readValue(json, AnalyzeImageResponseDto.class);

        assertThat(response.getTags()).containsEntry("sample", 0.9);
        assertThat(response.getEmbedding()).containsExactly(0.1, 0.2);
    }
}
