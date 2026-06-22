package com.tamakara.bakabooru.module.ai.client;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingRequestDto;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageResponseDto;
import com.tamakara.bakabooru.config.AiServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class AiServiceClient {

    private static final Duration AI_TIMEOUT = Duration.ofSeconds(120);

    private final WebClient webClient;

    public AiServiceClient(
            AiServiceProperties aiServiceProperties,
            WebClient.Builder webClientBuilder
    ) {
        this.webClient = webClientBuilder.baseUrl(aiServiceProperties.getUrl()).build();
    }

    public TagImageResponseDto tagImage(TagImageRequestDto requestBody) {
        return webClient
                .post()
                .uri("/tag/image")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TagImageResponseDto.class)
                .block(AI_TIMEOUT);
    }

    public ImageEmbeddingResponseDto imageEmbedding(ImageEmbeddingRequestDto requestBody) {
        return webClient
                .post()
                .uri("/embedding/image")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ImageEmbeddingResponseDto.class)
                .block(AI_TIMEOUT);
    }

    public ImageEmbeddingResponseDto imageEmbedding(MultipartFile file) {
        return webClient
                .post()
                .uri("/embedding/image-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file.getResource()))
                .retrieve()
                .bodyToMono(ImageEmbeddingResponseDto.class)
                .block(AI_TIMEOUT);
    }

    public EmbeddingResponseDto generateEmbedding(SemanticSearchRequestDto requestBody) {
        return webClient
                .post()
                .uri("/search/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbeddingResponseDto.class)
                .block(AI_TIMEOUT);
    }
}
