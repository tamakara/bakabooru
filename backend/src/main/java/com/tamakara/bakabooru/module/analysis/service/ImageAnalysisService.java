package com.tamakara.bakabooru.module.analysis.service;

import com.tamakara.bakabooru.module.analysis.dto.BatchTagsRequest;
import com.tamakara.bakabooru.module.analysis.dto.BatchVectorsRequest;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final AnalysisJobService analysisJobService;
    private final SystemSettingService settingService;

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
                ) {
            throw new IllegalArgumentException("图片列表不能为空");
        }
        validateImageIds(request.ids());
        return request.ids().stream().distinct()
                .map(this::enqueueTags)
                .toList();
    }

    @Transactional
    public List<ImageDto> enqueueVectors(BatchVectorsRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()
                ) {
            throw new IllegalArgumentException("图片列表不能为空");
        }
        validateImageIds(request.ids());
        String modelIds = requireActiveClip();
        return request.ids().stream().distinct()
                .map(id -> enqueue(id, null, modelIds))
                .toList();
    }

    @Transactional
    public ImageDto enqueueTags(Long imageId) {
        String modelId = settingService.getActiveTaggerId();
        if (modelId == null || modelId.isBlank()) throw new IllegalStateException("请先在模型管理中启用标签模型");
        return enqueue(imageId, modelId, null);
    }

    @Transactional
    public ImageDto enqueueVectors(Long imageId) {
        return enqueue(imageId, null, requireActiveClip());
    }

    private String requireActiveClip() {
        String modelId = settingService.getActiveClipId();
        if (modelId == null || modelId.isBlank()) throw new IllegalStateException("请先在模型管理中启用 CLIP 模型");
        return modelId;
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
