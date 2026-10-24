package com.tamakara.bakabooru.module.model.service;

import com.tamakara.bakabooru.module.model.client.ModelManagerClient;
import com.tamakara.bakabooru.module.model.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.model.dto.SemanticSearchRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryParsingService {

    private final ModelManagerClient modelManagerClient;

    public EmbeddingResponseDto generateEmbedding(String query) {
        return generateEmbedding(query, null);
    }

    public EmbeddingResponseDto generateEmbedding(String query, String modelId) {
        SemanticSearchRequestDto request = new SemanticSearchRequestDto();
        request.setQuery(query);
        request.setModelId(modelId);
        EmbeddingResponseDto response = modelManagerClient.generateEmbedding(request);
        if (response == null || response.getEmbedding() == null || response.getEmbedding().size() != 512) {
            throw new IllegalStateException("AI text embedding response must contain 512 values");
        }
        return response;
    }
}
