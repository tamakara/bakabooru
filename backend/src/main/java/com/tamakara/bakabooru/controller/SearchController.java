package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.search.dto.SearchRequestDto;
import com.tamakara.bakabooru.module.search.dto.SearchResultDto;
import com.tamakara.bakabooru.module.search.service.SearchService;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "API")
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    @Operation(summary = "operation")
    public SearchResultDto<ImageThumbnailDto> search(@RequestBody SearchRequestDto request) {
        return searchService.search(request);
    }

    @PostMapping(path = "/image", consumes = "multipart/form-data")
    @Operation(summary = "operation")
    public SearchResultDto<ImageThumbnailDto> searchByImage(
            @RequestPart("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0.7") Double threshold,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        return searchService.searchByImage(file, threshold, page, size);
    }
}
