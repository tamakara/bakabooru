package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiProcessingService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_READY = "READY";

    private final ImageRepository imageRepository;
    private final TagService tagService;
    private final EmbeddingService embeddingService;
    private final SystemSettingService systemSettingService;
    private final TransactionTemplate transactionTemplate;

    private final Executor aiExecutor;

    public AiProcessingService(
            ImageRepository imageRepository,
            TagService tagService,
            EmbeddingService embeddingService,
            SystemSettingService systemSettingService,
            TransactionTemplate transactionTemplate,
            @Qualifier("aiExecutor") Executor aiExecutor
    ) {
        this.imageRepository = imageRepository;
        this.tagService = tagService;
        this.embeddingService = embeddingService;
        this.systemSettingService = systemSettingService;
        this.transactionTemplate = transactionTemplate;
        this.aiExecutor = aiExecutor;
    }

    public Image requestProcessing(Long imageId) {
        AtomicBoolean shouldStart = new AtomicBoolean(false);
        Image image = transactionTemplate.execute(status -> {
            Image current = imageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("找不到图片"));

            if (STATUS_READY.equals(current.getAiStatus()) || STATUS_PROCESSING.equals(current.getAiStatus())) {
                return current;
            }

            current.setAiStatus(STATUS_PROCESSING);
            current.setAiError(null);
            current.setAiAttemptedAt(Instant.now());
            current.setAiCompletedAt(null);
            shouldStart.set(true);
            return imageRepository.save(current);
        });

        if (shouldStart.get()) {
            aiExecutor.execute(() -> processImage(imageId));
        }
        return image;
    }

    public void enqueuePendingImages() {
        transactionTemplate.executeWithoutResult(status -> {
            for (Image image : imageRepository.findByAiStatus(STATUS_PROCESSING)) {
                image.setAiStatus(STATUS_PENDING);
                imageRepository.save(image);
            }
        });
        enqueueAllPending();
    }

    /**
     * 批量入队所有待处理且无错误的图片
     * @return 入队数量
     */
    public int enqueueAllPending() {
        List<Image> pending = imageRepository.findByAiStatus(STATUS_PENDING);
        int count = 0;
        for (Image image : pending) {
            if (image.getAiError() == null || image.getAiError().isBlank()) {
                requestProcessing(image.getId());
                count++;
            }
        }
        return count;
    }

    private void processImage(Long imageId) {
        try {
            ProcessingInput input = transactionTemplate.execute(status -> {
                Image image = imageRepository.findById(imageId)
                        .orElseThrow(() -> new RuntimeException("找不到图片"));
                return new ProcessingInput(image.getId(), image.getHash());
            });

            String objectName = "original/" + input.hash();
            double threshold = systemSettingService.getDoubleSetting("tag.threshold");
            Map<String, Double> tags = tagService.tagImage(objectName, threshold);
            double[] embedding = embeddingService.generateImageEmbedding(objectName);

            transactionTemplate.executeWithoutResult(status -> completeProcessing(input.id(), tags, embedding));
            log.info("AI 处理完成 imageId={}", input.id());
        } catch (Exception e) {
            log.warn("AI 处理失败 imageId={}: {}", imageId, e.getMessage());
            transactionTemplate.executeWithoutResult(status -> markPendingWithError(imageId, e.getMessage()));
        }
    }

    private void completeProcessing(Long imageId, Map<String, Double> tags, double[] embedding) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("找不到图片"));
        image.setEmbedding(embedding);
        image.setAiStatus(STATUS_READY);
        image.setAiError(null);
        image.setAiCompletedAt(Instant.now());

        Set<Long> existingTagIds = image.getTagRelations().stream()
                .map(relation -> relation.getTag().getId())
                .collect(Collectors.toSet());

        for (Map.Entry<String, Double> entry : tags.entrySet()) {
            try {
                Tag tag = tagService.getTagByName(entry.getKey());
                if (!existingTagIds.contains(tag.getId())) {
                    image.addTag(tag, entry.getValue());
                    existingTagIds.add(tag.getId());
                }
            } catch (Exception e) {
                log.warn("跳过未知标签: {}", entry.getKey());
            }
        }

        imageRepository.save(image);
    }

    private void markPendingWithError(Long imageId, String error) {
        Image image = imageRepository.findById(imageId).orElse(null);
        if (image == null) return;
        image.setAiStatus(STATUS_PENDING);
        image.setAiError(error);
        image.setAiCompletedAt(null);
        imageRepository.save(image);
    }

    private record ProcessingInput(Long id, String hash) {
    }
}
