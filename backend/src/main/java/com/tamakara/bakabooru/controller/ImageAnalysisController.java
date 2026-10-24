package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.analysis.dto.BatchTagsRequest;
import com.tamakara.bakabooru.module.analysis.dto.BatchVectorsRequest;
import com.tamakara.bakabooru.module.analysis.dto.TagsRequest;
import com.tamakara.bakabooru.module.analysis.dto.VectorsRequest;
import com.tamakara.bakabooru.module.analysis.service.ImageAnalysisService;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;

    @PostMapping("/{id}/ai/retry")
    public ImageDto retry(@PathVariable Long id, @RequestParam(required = false) String capability) {
        return imageAnalysisService.retry(id, capability);
    }

    @PostMapping("/{id}/ai/tags")
    public ImageDto generateTags(@PathVariable Long id, @RequestBody TagsRequest request) {
        return imageAnalysisService.enqueue(id, request == null ? null : request.modelId(), null);
    }

    @PostMapping("/{id}/ai/vectors")
    public ImageDto generateVectors(@PathVariable Long id, @RequestBody VectorsRequest request) {
        List<String> models = request == null || request.modelIds() == null ? List.of() : request.modelIds();
        return imageAnalysisService.enqueue(id, null, String.join(",", models));
    }

    @PostMapping("/batch/ai/tags")
    public List<ImageDto> generateTagsBatch(@RequestBody BatchTagsRequest request) {
        try {
            return imageAnalysisService.enqueueTags(request);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }

    @PostMapping("/batch/ai/vectors")
    public List<ImageDto> generateVectorsBatch(@RequestBody BatchVectorsRequest request) {
        try {
            return imageAnalysisService.enqueueVectors(request);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(BAD_REQUEST, error.getMessage(), error);
        }
    }
}
