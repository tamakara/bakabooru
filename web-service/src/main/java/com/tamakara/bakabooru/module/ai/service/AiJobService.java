package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AiJobService {

    public static final String IMAGE_PENDING = "PENDING";
    public static final String IMAGE_PROCESSING = "PROCESSING";
    public static final String IMAGE_READY = "READY";
    public static final String IMAGE_FAILED = "FAILED";

    private final AiJobRepository aiJobRepository;
    private final ImageRepository imageRepository;

    @Transactional
    public AiJob enqueue(Image image) {
        return aiJobRepository.findByImageId(image.getId()).orElseGet(() -> {
            Instant now = Instant.now();
            AiJob job = new AiJob();
            job.setImage(image);
            job.setStatus(AiJobStatus.PENDING);
            job.setAttempts(0);
            job.setNextRetryAt(now);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            image.setAiStatus(IMAGE_PENDING);
            image.setAiError(null);
            image.setAiCompletedAt(null);
            return aiJobRepository.save(job);
        });
    }

    @Transactional
    public Image retry(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("图片不存在"));
        if (!IMAGE_FAILED.equals(image.getAiStatus())) {
            throw new IllegalStateException("只有 AI 处理失败的图片可以重试");
        }

        AiJob job = aiJobRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalStateException("AI 任务不存在"));
        Instant now = Instant.now();
        job.setStatus(AiJobStatus.PENDING);
        job.setAttempts(0);
        job.setNextRetryAt(now);
        job.setLockedBy(null);
        job.setLockedUntil(null);
        job.setErrorMessage(null);
        job.setCompletedAt(null);
        job.setUpdatedAt(now);

        image.setAiStatus(IMAGE_PENDING);
        image.setAiError(null);
        image.setAiCompletedAt(null);
        aiJobRepository.save(job);
        return imageRepository.save(image);
    }
}
