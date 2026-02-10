package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.service.ImageService;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "图库", description = "图片库操作")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "获取图片列表", description = "获取分页的图片列表")
    public Page<ImageDto> listImages(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return imageService.listImages(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图片详情")
    public ImageDto getImage(@PathVariable Long id) {
        return imageService.getImage(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片")
    public void deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新图片详情")
    public ImageDto updateImage(@PathVariable Long id, @RequestBody ImageDto dto) {
        return imageService.updateImage(id, dto);
    }

    @PostMapping("/{id}/tags/regenerate")
    @Operation(summary = "重新生成标签")
    public ImageDto regenerateTags(@PathVariable Long id) {
        return imageService.regenerateTags(id);
    }

    @PostMapping("/{id}/tags")
    @Operation(summary = "添加标签")
    public ImageDto addTag(@PathVariable Long id, @RequestBody TagDto tagDto) {
        return imageService.addTag(id, tagDto);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "移除标签")
    public ImageDto removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return imageService.removeTag(id, tagId);
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除图片")
    public void deleteImages(@RequestBody java.util.List<Long> ids) {
        imageService.deleteImages(ids);
    }

    @PostMapping("/batch/download")
    @Operation(summary = "批量下载图片 (ZIP)")
    public void downloadImages(@RequestBody java.util.List<Long> ids, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"images.zip\"");
        imageService.downloadImages(ids, response.getOutputStream());
    }
}
