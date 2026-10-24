package com.tamakara.bakabooru.module.model.service;

import com.tamakara.bakabooru.module.model.entity.AnalysisJob;
import com.tamakara.bakabooru.module.model.entity.AnalysisJobStatus;
import com.tamakara.bakabooru.module.model.repository.AnalysisJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.entity.ImageEmbedding;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AnalysisJobService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ImageRepository imageRepository;

    @Transactional
    public AnalysisJob enqueue(Image image) {
        return enqueue(image, null, null);
    }

    @Transactional
    public AnalysisJob enqueue(Image image, String tagModelId, String vectorModelIds) {
        if (tagModelId != null && !tagModelId.isBlank() && vectorModelIds != null && !vectorModelIds.isBlank()) {
            enqueue(image, tagModelId, null);
            return enqueue(image, null, vectorModelIds);
        }
        if (vectorModelIds != null && !vectorModelIds.isBlank()) {
            for (String rawModelId : vectorModelIds.split(",")) {
                String modelId = rawModelId.trim();
                image.getIndexVectors().stream().filter(vector -> modelId.equals(vector.getModelId())).findFirst().ifPresentOrElse(vector -> {
                    vector.setStatus("PENDING");
                    vector.setErrorMessage(null);
                }, () -> {
                    ImageEmbedding vector = new ImageEmbedding();
                    vector.setImage(image);
                    vector.setModelId(modelId);
                    vector.setModelRevision("1");
                    vector.setStatus("PENDING");
                    image.getIndexVectors().add(vector);
                });
            }
        }
        String capability = tagModelId != null && !tagModelId.isBlank() && vectorModelIds != null && !vectorModelIds.isBlank()
                ? "TAGS_AND_VECTORS" : tagModelId != null && !tagModelId.isBlank() ? "TAGS" : "VECTORS";
        return analysisJobRepository.findFirstByImageIdAndCapabilityOrderByUpdatedAtDesc(image.getId(), capability).map(existing -> {
            Instant now = Instant.now();
            existing.setTagModelId(tagModelId);
            existing.setVectorModelIds(vectorModelIds);
            existing.setCapability(capability);
            existing.setStatus(AnalysisJobStatus.PENDING);
            existing.setAttempts(0);
            existing.setNextRetryAt(now);
            existing.setLockedBy(null);
            existing.setLockedUntil(null);
            existing.setErrorMessage(null);
            existing.setCompletedAt(null);
            existing.setUpdatedAt(now);
            AnalysisJob saved = analysisJobRepository.save(existing);
            markAnalyzing(image);
            return saved;
        }).orElseGet(() -> {
            Instant now = Instant.now();
            AnalysisJob job = new AnalysisJob();
            job.setImage(image);
            job.setStatus(AnalysisJobStatus.PENDING);
            job.setAttempts(0);
            job.setNextRetryAt(now);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            job.setTagModelId(tagModelId);
            job.setVectorModelIds(vectorModelIds);
            job.setCapability(capability);
            AnalysisJob saved = analysisJobRepository.save(job);
            markAnalyzing(image);
            return saved;
        });
    }

    @Transactional
    public Image retry(Long imageId) {
        return retry(imageId, null);
    }

    @Transactional
    public Image retry(Long imageId, String capability) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found: " + imageId));
        var jobQuery = capability == null || capability.isBlank()
                ? analysisJobRepository.findFirstByImageIdOrderByCreatedAtDesc(imageId)
                : analysisJobRepository.findFirstByImageIdAndCapabilityOrderByUpdatedAtDesc(imageId, capability.toUpperCase());
        AnalysisJob job = jobQuery
                .orElseThrow(() -> new IllegalStateException("AI job not found: " + imageId));
        Instant now = Instant.now();
        job.setStatus(AnalysisJobStatus.PENDING);
        job.setAttempts(0);
        job.setNextRetryAt(now);
        job.setLockedBy(null);
        job.setLockedUntil(null);
        job.setErrorMessage(null);
        job.setCompletedAt(null);
        job.setUpdatedAt(now);
        analysisJobRepository.save(job);
        markAnalyzing(image);
        return imageRepository.save(image);
    }

    private void markAnalyzing(Image image) {
        image.setAnalysisError(null);
        image.setStatus("ANALYZING");
        image.setAnalysisStage(activeStage(image.getId()));
        image.setAnalysisCompletedAt(null);
    }

    private String activeStage(Long imageId) {
        boolean tags = false;
        boolean vectors = false;
        for (AnalysisJob job : analysisJobRepository.findAllByImageId(imageId)) {
            if (job.getStatus() != AnalysisJobStatus.PENDING && job.getStatus() != AnalysisJobStatus.RUNNING) continue;
            tags |= "TAGS".equals(job.getCapability()) || "TAGS_AND_VECTORS".equals(job.getCapability());
            vectors |= "VECTORS".equals(job.getCapability()) || "TAGS_AND_VECTORS".equals(job.getCapability());
        }
        return tags && vectors ? "TAGS_AND_VECTORS" : tags ? "TAGS" : "VECTORS";
    }
}


