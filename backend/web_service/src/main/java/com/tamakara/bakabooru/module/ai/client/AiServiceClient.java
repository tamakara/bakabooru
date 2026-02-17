package com.tamakara.bakabooru.module.ai.client;

import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingRequestDto;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchResponseDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AiServiceClient {

    private final WebClient webClient;

    public AiServiceClient(
            @Value("${app.ai-service.url}") String aiServiceUrl,
            WebClient.Builder webClientBuilder
    ) {
        this.webClient = webClientBuilder.baseUrl(aiServiceUrl).build();
    }

    public Void initTags() {
        return webClient
                .post()
                .uri("/tags/init")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public TagImageResponseDto tagImage(TagImageRequestDto requestBody) {
        return webClient
                .post()
                .uri("/tag/image")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TagImageResponseDto.class)
                .block();
    }

    public SemanticSearchResponseDto semanticSearch(SemanticSearchRequestDto requestBody) {
        return webClient
                .post()
                .uri("/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(SemanticSearchResponseDto.class)
                .block();
    }

    public ImageEmbeddingResponseDto imageEmbedding(ImageEmbeddingRequestDto requestBody) {
        return webClient
                .post()
                .uri("/embedding/image")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ImageEmbeddingResponseDto.class)
                .block();
    }
}
