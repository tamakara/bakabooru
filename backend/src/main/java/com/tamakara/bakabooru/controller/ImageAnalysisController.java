package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.module.analysis.dto.BatchTagsRequest;
import com.tamakara.bakabooru.module.analysis.dto.BatchVectorsRequest;
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
    public ImageDto generateTags(@PathVariable Long id) {
        return imageAnalysisService.enqueueTags(id);
    }

    @PostMapping("/{id}/ai/vectors")
    public ImageDto generateVectors(@PathVariable Long id) {
        return imageAnalysisService.enqueueVectors(id);
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
