package com.tamakara.bakabooru.module.gallery.controller;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.AiTagsRequest;
import com.tamakara.bakabooru.module.image.dto.AiVectorsRequest;
import com.tamakara.bakabooru.module.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 闁搞儱澧芥晶鏍不閿涘嫭鍊為柟璨夊啫鐓戦柛?
 * 濠㈣泛瀚幃濠囧炊閸撗冾暬闁汇劌瀚·鍐礆閻樿櫕鏆柡灞诲劘閳ь兛鐒﹂悥锝囩驳閸撗屽悁闁荤偛妫楀鐑藉箥瑜版帒娅ら柟鍨С缂?
 */
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
    public ImageDto retryAiProcessing(@PathVariable Long id) {
        return imageService.retryAiProcessing(id);
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
