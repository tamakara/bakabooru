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

    private final AiJobRepository aiJobRepository;
    private final ImageRepository imageRepository;

    @Transactional
    public AiJob enqueue(Image image) {
        return enqueue(image, null, null);
    }

    @Transactional
    public AiJob enqueue(Image image, String tagModelId, String vectorModelIds) {
        return aiJobRepository.findByImageId(image.getId()).map(existing -> {
            Instant now = Instant.now();
            existing.setTagModelId(tagModelId);
            existing.setVectorModelIds(vectorModelIds);
            existing.setStatus(AiJobStatus.PENDING);
            existing.setAttempts(0);
            existing.setNextRetryAt(now);
            existing.setLockedBy(null);
            existing.setLockedUntil(null);
            existing.setErrorMessage(null);
            existing.setCompletedAt(null);
            existing.setUpdatedAt(now);
            image.setAiError(null);
            image.setAiCompletedAt(null);
            return aiJobRepository.save(existing);
        }).orElseGet(() -> {
            Instant now = Instant.now();
            AiJob job = new AiJob();
            job.setImage(image);
            job.setStatus(AiJobStatus.PENDING);
            job.setAttempts(0);
            job.setNextRetryAt(now);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            image.setAiError(null);
            image.setAiCompletedAt(null);
            job.setTagModelId(tagModelId);
            job.setVectorModelIds(vectorModelIds);
            return aiJobRepository.save(job);
        });
    }

    @Transactional
    public Image retry(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found: " + imageId));
        AiJob job = aiJobRepository.findByImageId(imageId)
                .orElseThrow(() -> new IllegalStateException("AI job not found: " + imageId));
        Instant now = Instant.now();
        job.setStatus(AiJobStatus.PENDING);
        job.setAttempts(0);
        job.setNextRetryAt(now);
        job.setLockedBy(null);
        job.setLockedUntil(null);
        job.setErrorMessage(null);
        job.setCompletedAt(null);
        job.setUpdatedAt(now);
        image.setAiError(null);
        image.setAiCompletedAt(null);
        aiJobRepository.save(job);
        return imageRepository.save(image);
    }
}


