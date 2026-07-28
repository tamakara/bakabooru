package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParseQueryService {

    private final AiServiceClient aiServiceClient;

    public EmbeddingResponseDto generateEmbedding(String query) {
        SemanticSearchRequestDto request = new SemanticSearchRequestDto();
        request.setQuery(query);
        EmbeddingResponseDto response = aiServiceClient.generateEmbedding(request);
        if (response == null || response.getEmbedding() == null || response.getEmbedding().size() != 512) {
            throw new IllegalStateException("AI text embedding response must contain 512 values");
        }
        return response;
    }
}
