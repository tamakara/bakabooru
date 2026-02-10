package com.tamakara.bakabooru.module.gallery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamakara.bakabooru.module.ai.dto.QueryParseRequestDto;
import com.tamakara.bakabooru.module.ai.service.AiService;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryParseService {

    private final AiService aiService;
    private final SystemSettingService systemSettingService;

    public String queryParse(String query) {
        String llmUrl = systemSettingService.getSetting("llm.url", "");
        String llmModel = systemSettingService.getSetting("llm.model", "");
        String llmApiKey = systemSettingService.getSetting("llm.api-key", "");

        if (!StringUtils.hasText(llmUrl) || !StringUtils.hasText(llmModel) || !StringUtils.hasText(llmApiKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");
        }
        QueryParseRequestDto requestDto = new QueryParseRequestDto();
        requestDto.setQuery(query);
        requestDto.setLlmUrl(llmUrl);
        requestDto.setLlmModel(llmModel);
        requestDto.setLlmApiKey(llmApiKey);

        return aiService.queryParse(requestDto);
    }
}
