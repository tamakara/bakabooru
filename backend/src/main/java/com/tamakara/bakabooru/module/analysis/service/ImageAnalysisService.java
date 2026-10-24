package com.tamakara.bakabooru.module.analysis.service;

import com.tamakara.bakabooru.module.analysis.dto.BatchTagsRequest;
import com.tamakara.bakabooru.module.analysis.dto.BatchVectorsRequest;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private static final String TAGGER_MODEL_ID = "camie-tagger-v2";
    private static final Set<String> VECTOR_MODEL_IDS = Set.of("clip-vit-base-patch32");

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final AnalysisJobService analysisJobService;

    @Transactional
    public ImageDto retry(Long imageId, String capability) {
        return imageMapper.toDto(analysisJobService.retry(imageId, capability));
    }

    @Transactional
    public ImageDto enqueue(Long imageId, String tagModelId, String vectorModelIds) {
        Image image = findImage(imageId);
        validateModels(tagModelId, vectorModelIds);
        analysisJobService.enqueue(image, tagModelId, vectorModelIds);
        return imageMapper.toDto(image);
    }

    @Transactional
    public List<ImageDto> enqueueTags(BatchTagsRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()
                || request.modelId() == null || request.modelId().isBlank()) {
            throw new IllegalArgumentException("图片列表和标签模型不能为空");
        }
        validateImageIds(request.ids());
        return request.ids().stream().distinct()
                .map(id -> enqueue(id, request.modelId(), null))
                .toList();
    }

    @Transactional
    public List<ImageDto> enqueueVectors(BatchVectorsRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()
                || request.modelIds() == null || request.modelIds().stream().allMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("图片列表和向量模型不能为空");
        }
        validateImageIds(request.ids());
        String modelIds = request.modelIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElseThrow(() -> new IllegalArgumentException("至少选择一个向量模型"));
        return request.ids().stream().distinct()
                .map(id -> enqueue(id, null, modelIds))
                .toList();
    }

    private Image findImage(Long imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found: " + imageId));
    }

    private void validateModels(String tagModelId, String vectorModelIds) {
        if ((tagModelId == null || tagModelId.isBlank())
                && (vectorModelIds == null || vectorModelIds.isBlank())) {
            throw new IllegalArgumentException("At least one analysis model is required");
        }
        if (tagModelId != null && !tagModelId.isBlank() && !TAGGER_MODEL_ID.equals(tagModelId.trim())) {
            throw new IllegalArgumentException("未知的标签模型: " + tagModelId);
        }
        if (vectorModelIds != null && !vectorModelIds.isBlank()) {
            for (String modelId : vectorModelIds.split(",")) {
                if (!VECTOR_MODEL_IDS.contains(modelId.trim())) {
                    throw new IllegalArgumentException("未知的向量模型: " + modelId);
                }
            }
        }
    }

    private void validateImageIds(List<Long> ids) {
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("图片 ID 必须为正数");
        }
        List<Long> existingIds = imageRepository.findAllById(ids).stream().map(Image::getId).toList();
        if (existingIds.size() != ids.stream().distinct().count()) {
            throw new IllegalArgumentException("部分图片不存在");
        }
    }
}
