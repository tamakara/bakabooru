package com.tamakara.bakabooru.module.ai.client;

import com.tamakara.bakabooru.module.ai.dto.ParseQueryRequestDto;
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
                .uri("/init_tags")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    public TagImageResponseDto tagImage(TagImageRequestDto requestBody) {

        return webClient
                .post()
                .uri("/tag_image")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TagImageResponseDto.class)
                .block(); // 同步等结果
    }

    public String parseQuery(ParseQueryRequestDto requestBody) {

        return webClient
                .post()
                .uri("/parse_query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 同步等结果
    }
}
