package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.ai.service.AiProcessingService;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 图片管理控制器
 * 处理图片的增删改查、标签管理及批量操作
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "图库管理", description = "图片库核心操作")
public class ImageController {

    private final ImageService imageService;
    private final AiProcessingService aiProcessingService;

    @GetMapping("/{id}")
    @Operation(summary = "获取详情", description = "获取图片详细信息并增加查看次数")
    public ImageDto getImage(@PathVariable Long id) {
        return imageService.getImage(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除图片")
    public void deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新信息")
    public ImageDto updateImage(@PathVariable Long id, @RequestBody ImageDto dto) {
        return imageService.updateImage(id, dto);
    }

    @PostMapping("/{id}/tags/{tagId}")
    @Operation(summary = "添加标签")
    public ImageDto addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return imageService.addTag(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    @Operation(summary = "移除标签")
    public ImageDto removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return imageService.removeTag(id, tagId);
    }

    @PostMapping("/{id}/ai/retry")
    @Operation(summary = "重试 AI 处理")
    public ImageDto retryAiProcessing(@PathVariable Long id) {
        return imageService.retryAiProcessing(id);
    }

    @PostMapping("/ai/enqueue-all")
    @Operation(summary = "批量触发 AI 处理", description = "入队所有待处理且无错误的图片进行 AI 后处理")
    public Map<String, Integer> enqueueAllAi() {
        int count = aiProcessingService.enqueueAllPending();
        return Map.of("enqueued", count);
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "批量删除")
    public void deleteImages(@RequestBody List<Long> ids) {
        imageService.deleteImages(ids);
    }

    @PostMapping("/batch/download")
    @Operation(summary = "批量下载")
    public void downloadImages(@RequestBody List<Long> ids, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"images.zip\"");
        imageService.downloadImages(ids, response.getOutputStream());
    }
}
