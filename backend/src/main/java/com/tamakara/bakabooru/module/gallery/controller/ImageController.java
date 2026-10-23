package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.AiTagsRequest;
import com.tamakara.bakabooru.module.image.dto.AiVectorsRequest;
import com.tamakara.bakabooru.module.image.dto.BatchAiTagsRequest;
import com.tamakara.bakabooru.module.image.dto.BatchAiVectorsRequest;
import com.tamakara.bakabooru.module.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** 图片详情、标签和 AI 任务接口。 */
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

    @PostMapping("/{id}/ai/retry")
    @Operation(summary = "operation")
    public ImageDto retryAiProcessing(@PathVariable Long id, @RequestParam(required = false) String capability) {
        return imageService.retryAiProcessing(id, capability);
    }

    @PostMapping("/{id}/ai/tags")
    public ImageDto generateTags(@PathVariable Long id, @RequestBody AiTagsRequest request) {
        return imageService.enqueueAi(id, request == null ? null : request.modelId(), null);
    }

    @PostMapping("/{id}/ai/vectors")
    public ImageDto generateVectors(@PathVariable Long id, @RequestBody AiVectorsRequest request) {
        List<String> models = request == null || request.modelIds() == null ? List.of() : request.modelIds();
        return imageService.enqueueAi(id, null, String.join(",", models));
    }

    @PostMapping("/batch/delete")
    @Operation(summary = "operation")
    public void deleteImages(@RequestBody List<Long> ids) {
        imageService.deleteImages(ids);
    }

    @PostMapping("/batch/ai/tags")
    public List<ImageDto> generateTagsBatch(@RequestBody BatchAiTagsRequest request) {
        try {
            return imageService.enqueueBatchTags(request);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }

    @PostMapping("/batch/ai/vectors")
    public List<ImageDto> generateVectorsBatch(@RequestBody BatchAiVectorsRequest request) {
        try {
            return imageService.enqueueBatchVectors(request);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
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
