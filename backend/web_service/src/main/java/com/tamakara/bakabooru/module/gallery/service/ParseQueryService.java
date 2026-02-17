package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.dto.SemanticSearchRequestDto;
import com.tamakara.bakabooru.module.ai.dto.SemanticSearchResponseDto;
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

    /**
     * 解析语义描述，返回语义搜索结果
     */
    public SemanticSearchResponseDto parseSemanticQuery(String query) {
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

        SemanticSearchResponseDto response = aiServiceClient.semanticSearch(requestDto);

        if (!response.isSuccess()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "语义搜索解析失败: " + response.getError());
        }

        return response;
    }

    /**
     * 解析语义描述，返回标签字符串（兼容旧接口）
     */
    public String parseQuery(String query) {
        SemanticSearchResponseDto response = parseSemanticQuery(query);

        StringBuilder result = new StringBuilder();
        if (response.getTags() != null) {
            // 添加正向标签
            if (response.getTags().getPositive() != null) {
                for (String tag : response.getTags().getPositive()) {
                    if (!result.isEmpty()) result.append(" ");
                    result.append(tag);
                }
            }
            // 添加负向标签（带 - 前缀）
            if (response.getTags().getNegative() != null) {
                for (String tag : response.getTags().getNegative()) {
                    if (!result.isEmpty()) result.append(" ");
                    result.append("-").append(tag);
                }
            }
        }
        return result.toString();
    }
}
