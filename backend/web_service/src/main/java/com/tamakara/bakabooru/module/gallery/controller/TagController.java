package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理控制器
 * 处理标签的检索
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "标签检索与维护")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "获取标签", description = "获取所有标签或按关键词搜索")
    public List<TagDto> listTags(@RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            return tagService.searchTags(query);
        }
        return tagService.listTags();
    }
}
