package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParseQueryService {

    private final AiServiceClient aiServiceClient;

    public EmbeddingResponseDto generateEmbedding(String query) {
        SemanticSearchRequestDto requestDto = buildRequest(query);
        EmbeddingResponseDto response = aiServiceClient.generateEmbedding(requestDto);
        if (!response.isSuccess()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "向量生成失败: " + response.getError());
        }
        return response;
    }

    private SemanticSearchRequestDto buildRequest(String query) {
        SemanticSearchRequestDto requestDto = new SemanticSearchRequestDto();
        requestDto.setQuery(query);
        return requestDto;
    }
}
