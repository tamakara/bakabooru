package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


/** 图片详情、标签和文件操作接口。 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "API")
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/{id}")
    @Operation(summary = "operation")
    public ImageDto getImage(@PathVariable Long id) {
        return imageService.getImage(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "operation")
    public void deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "operation")
    public ImageDto updateImage(@PathVariable Long id, @RequestBody ImageDto dto) {
        return imageService.updateImage(id, dto);
    }

    @PostMapping("/{id}/tags/{tagId}")
    @Operation(summary = "operation")
    public ImageDto addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return imageService.addTag(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "operation")
    public ImageDto removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return imageService.removeTag(id, tagId);
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "operation")
    public void deleteImages(@RequestBody List<Long> ids) {
        imageService.deleteImages(ids);
    }

    @PostMapping("/batch/delete-missing")
    @Operation(summary = "operation")
    public int deleteMissingImages() {
        return imageService.deleteMissingImages();
    }

    @PostMapping("/batch/download")
    @Operation(summary = "operation")
    public void downloadImages(@RequestBody List<Long> ids, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"images.zip\"");
        imageService.downloadImages(ids, response.getOutputStream());
    }
}
