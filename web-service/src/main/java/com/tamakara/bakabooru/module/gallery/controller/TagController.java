package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 閺嶅洨顒风粻锛勬倞閹貉冨煑閸?
 * 婢跺嫮鎮婇弽鍥╊劮閻ㄥ嫭顥呯槐?
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "API")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "operation")
    public List<TagDto> listTags(@RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            return tagService.searchTags(query);
        }
        return tagService.listTags();
    }
}
