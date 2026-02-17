package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.dto.EmbeddingResponseDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.module.ai.dto.TagsResponseDto;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParseQueryService {

    private final AiServiceClient aiServiceClient;
    private final SystemSettingService systemSettingService;

    public TagsResponseDto extractTags(String query) {
        SemanticSearchRequestDto requestDto = buildRequest(query);
        TagsResponseDto response = aiServiceClient.extractTags(requestDto);
        if (!response.isSuccess()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "标签提取失败: " + response.getError());
        }
        return response;
    }

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
        String llmUrl = systemSettingService.getSetting("llm.url");
        String llmModel = systemSettingService.getSetting("llm.model");
        String llmApiKey = systemSettingService.getSetting("llm.api-key");

        if (!StringUtils.hasText(llmUrl) || !StringUtils.hasText(llmModel) || !StringUtils.hasText(llmApiKey)) {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");
        }

        SemanticSearchRequestDto requestDto = new SemanticSearchRequestDto();
        requestDto.setQuery(query);
        requestDto.setLlmUrl(llmUrl);
        requestDto.setLlmModel(llmModel);
        requestDto.setLlmApiKey(llmApiKey);
        return requestDto;
    }
}
