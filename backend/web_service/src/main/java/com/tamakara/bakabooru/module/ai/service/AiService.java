package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.dto.QueryParseRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagImageResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiService {

    private final WebClient webClient;

    public AiService(
            @Value("${app.ai-service.url}") String aiServiceUrl,
            WebClient.Builder webClientBuilder
    ) {
        this.webClient = webClientBuilder.baseUrl(aiServiceUrl).build();
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

    public String queryParse(QueryParseRequestDto requestBody) {

        return webClient
                .post()
                .uri("/query_parse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 同步等结果
    }
}
