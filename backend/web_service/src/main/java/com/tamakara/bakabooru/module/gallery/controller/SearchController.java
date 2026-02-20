package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.gallery.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.gallery.service.SearchService;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索", description = "高级搜索操作")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "搜索图片", description = "使用标签进行高级搜索")
    public Page<ImageThumbnailDto> search(@RequestBody SearchRequestDto request) {
        return searchService.search(request);
    }

    @PostMapping(path = "/image", consumes = "multipart/form-data")
    @Operation(summary = "以图搜图", description = "上传图片搜相似图片")
    public Page<ImageThumbnailDto> searchByImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "0.7") Double threshold,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        return searchService.searchByImage(file, threshold, page, size);
    }
}
