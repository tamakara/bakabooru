package com.tamakara.bakabooru.module.ai.client;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageResponseDto;
import com.tamakara.bakabooru.module.ai.dto.ImageEmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
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

    public AnalyzeImageResponseDto analyzeImage(AnalyzeImageRequestDto requestBody) {
        return webClient
                .post()
                .uri("/v1/images/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AnalyzeImageResponseDto.class)
                .block(AI_TIMEOUT);
    }

    public ImageEmbeddingResponseDto imageEmbedding(MultipartFile file) {
        return webClient
                .post()
                .uri("/v1/embeddings/image-file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", file.getResource()))
                .retrieve()
                .bodyToMono(ImageEmbeddingResponseDto.class)
                .block(AI_TIMEOUT);
    }

    public EmbeddingResponseDto generateEmbedding(SemanticSearchRequestDto requestBody) {
        return webClient
                .post()
                .uri("/v1/embeddings/text")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(EmbeddingResponseDto.class)
                .block(AI_TIMEOUT);
    }
}
