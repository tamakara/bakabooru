package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.ai.service.AiJobService;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.BatchAiTagsRequest;
import com.tamakara.bakabooru.module.image.dto.BatchAiVectorsRequest;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final String CAMIE_TAGGER_MODEL_ID = "camie-tagger-v2";
    private static final Set<String> CLIP_MODEL_IDS = Set.of("clip-vit-base-patch32");

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final StorageService storageService;
    private final TagService tagService;
    private final AiJobService aiJobService;

    @Transactional
    public ImageDto getImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        // 查看详情时记录访问次数。
        image.setViewCount(image.getViewCount() + 1);
        imageRepository.save(image);

        return imageMapper.toDto(image);
    }

    @Transactional
    public Image addImage(Image image) {
        return imageRepository.save(image);
    }

    public boolean existImageByHash(String hash) {
        return imageRepository.findByHash(hash).isPresent();
    }

    @Transactional
    public ImageDto updateImage(Long id, ImageDto dto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(Instant.now());
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto addTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        Tag tag = tagService.getTagById(tagId);
        image.getTagRelations().stream()
                .filter(relation -> relation.getTag().getId().equals(tagId))
                .findFirst()
                .ifPresentOrElse(relation -> {
                    relation.setSourceType("MANUAL");
                    relation.setScore(1.0);
                }, () -> image.addTag(tag, 1.0));
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        Tag tag = tagService.getTagById(tagId);
        image.deleteTag(tag);
        image.setUpdatedAt(Instant.now());

        return imageMapper.toDto(imageRepository.save(image));
    }

    public ImageDto retryAiProcessing(Long id) {
        return retryAiProcessing(id, null);
    }

    public ImageDto retryAiProcessing(Long id, String capability) {
        Image image = aiJobService.retry(id, capability);
        return imageMapper.toDto(image);
    }

    @Transactional
    public ImageDto enqueueAi(Long id, String tagModelId, String vectorModelIds) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        if ((tagModelId == null || tagModelId.isBlank())
                && (vectorModelIds == null || vectorModelIds.isBlank())) {
            throw new IllegalArgumentException("At least one AI model is required");
        }
        if (tagModelId != null && !tagModelId.isBlank() && !CAMIE_TAGGER_MODEL_ID.equals(tagModelId.trim())) {
            throw new IllegalArgumentException("未知的标签模型: " + tagModelId);
        }
        if (vectorModelIds != null && !vectorModelIds.isBlank()) {
            for (String modelId : vectorModelIds.split(",")) {
                if (!CLIP_MODEL_IDS.contains(modelId.trim())) {
                    throw new IllegalArgumentException("未知的向量模型: " + modelId);
                }
            }
        }
        aiJobService.enqueue(image, tagModelId, vectorModelIds);
        return imageMapper.toDto(image);
    }

    @Transactional
    public List<ImageDto> enqueueBatchTags(BatchAiTagsRequest request) {
        if (request == null || request.ids() == null || request.ids().isEmpty()
                || request.modelId() == null || request.modelId().isBlank()) {
            throw new IllegalArgumentException("图片列表和标签模型不能为空");
        }
        validateImageIds(request.ids());
        return request.ids().stream()
                .distinct()
                .map(id -> enqueueAi(id, request.modelId(), null))
                .toList();
    }

    @Transactional
    public List<ImageDto> enqueueBatchVectors(BatchAiVectorsRequest request) {
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
        return request.ids().stream()
                .distinct()
                .map(id -> enqueueAi(id, null, modelIds))
                .toList();
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

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        String objectName = "original/" + image.getHash();
        storageService.deleteFile(objectName);

        imageRepository.delete(image);
    }


    @Transactional
    public void deleteImages(List<Long> ids) {
        ids.forEach(id -> {
            try {
                deleteImage(id);
            } catch (Exception e) {
                throw new RuntimeException("删除图片失败 (ID: " + id + "): " + e.getMessage(), e);
            }
        });
    }

    @Transactional
    public int deleteMissingImages() {
        List<Image> missing = imageRepository.findByStatus("MISSING");
        for (Image image : missing) {
            try {
                storageService.deleteFile("original/" + image.getHash());
                storageService.deleteFile("thumbnail/" + image.getHash());
            } catch (Exception ignored) {
                // Missing objects are ignored; the database record is still removed.
            }
        }
        return missing.size();
    }

    @Transactional(readOnly = true)
    public void downloadImages(List<Long> ids, OutputStream outputStream) {
        List<Image> images = imageRepository.findAllById(ids);
        if (images.isEmpty()) return;

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Image image : images) {
                String objectName = "original/" + image.getHash();
                File file = storageService.getFile(objectName);
                if (file.exists()) {
                    // 使用图片 ID 前缀避免归档中同名文件冲突。
                    String fileName = String.format("%d_%s.%s", image.getId(), image.getTitle(), image.getExtension());
                    zos.putNextEntry(new ZipEntry(fileName));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("打包下载图片失败: " + e.getMessage(), e);
        }
    }
}
