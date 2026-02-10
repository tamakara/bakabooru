package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.gallery.service.QueryParseService;
import com.tamakara.bakabooru.module.gallery.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索", description = "高级搜索操作")
public class SearchController {

    private final SearchService searchService;
    private final QueryParseService queryParseService;

    @PostMapping
    @Operation(summary = "搜索图片", description = "使用标签进行高级搜索")
    public Page<ImageDto> search(@RequestBody SearchRequestDto request) {
        return searchService.search(request);
    }

    @PostMapping("/parse")
    @Operation(summary = "智能解析配置", description = "使用 LLM 解析自然语言并返回搜索配置")
    public String queryParse(@RequestBody  String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        return queryParseService.queryParse(query);
    }
}
