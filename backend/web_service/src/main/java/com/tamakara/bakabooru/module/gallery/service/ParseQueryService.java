package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.dto.ParseQueryRequestDto;
import com.tamakara.bakabooru.module.ai.service.AiService;
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

    private final AiService aiService;
    private final SystemSettingService systemSettingService;

    public String parseQuery(String query) {
        String llmUrl = systemSettingService.getSetting("llm.url");
        String llmModel = systemSettingService.getSetting("llm.model");
        String llmApiKey = systemSettingService.getSetting("llm.api-key");

        if (!StringUtils.hasText(llmUrl) || !StringUtils.hasText(llmModel) || !StringUtils.hasText(llmApiKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未完成LLM配置，请在设置中配置 LLM");
        }
        ParseQueryRequestDto requestDto = new ParseQueryRequestDto();
        requestDto.setQuery(query);
        requestDto.setLlmUrl(llmUrl);
        requestDto.setLlmModel(llmModel);
        requestDto.setLlmApiKey(llmApiKey);

        return aiService.parseQuery(requestDto);
    }
}
