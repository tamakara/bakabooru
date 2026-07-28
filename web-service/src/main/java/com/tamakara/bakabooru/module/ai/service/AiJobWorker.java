package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.config.AiJobProperties;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageResponseDto;
import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobWorker {

    private final AiJobRepository aiJobRepository;
    private final ImageRepository imageRepository;
    private final AiServiceClient aiServiceClient;
    private final TagService tagService;
    private final SystemSettingService systemSettingService;
    private final AiJobProperties properties;
    private final TransactionTemplate transactionTemplate;

    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(
            fixedDelayString = "${app.ai-job.poll-interval-ms:1000}",
            initialDelayString = "${app.ai-job.initial-delay-ms:2000}"
    )
    public void processAvailableJobs() {
        Long jobId;
        while ((jobId = claimNextJob()) != null) {
            processJob(jobId);
        }
    }

    @Scheduled(fixedRateString = "${app.ai-job.heartbeat-interval-ms:30000}")
    public void extendActiveLocks() {
        Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> aiJobRepository.extendWorkerLocks(
                workerId,
                AiJobStatus.RUNNING,
                now.plus(properties.getLockDuration()),
                now
        ));
    }

    Long claimNextJob() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            return aiJobRepository.findNextClaimable(now)
                    .map(job -> {
                        job.setStatus(AiJobStatus.RUNNING);
                        job.setAttempts(job.getAttempts() + 1);
                        job.setLockedBy(workerId);
                        job.setLockedUntil(now.plus(properties.getLockDuration()));
                        job.setUpdatedAt(now);

                        Image image = job.getImage();
                        image.setAiStatus(AiJobService.IMAGE_PROCESSING);
                        image.setAiError(null);
                        image.setAiAttemptedAt(now);
                        image.setAiCompletedAt(null);
                        imageRepository.save(image);
                        aiJobRepository.saveAndFlush(job);
                        return job.getId();
                    })
                    .orElse(null);
        });
    }

    void processJob(Long jobId) {
        try {
            ProcessingInput input = transactionTemplate.execute(status -> aiJobRepository.findById(jobId)
                    .map(job -> new ProcessingInput(job.getImage().getHash()))
                    .orElseThrow(() -> new IllegalStateException("AI 任务不存在")));
            double threshold = systemSettingService.getDoubleSetting("tag.threshold");
            AnalyzeImageResponseDto response = aiServiceClient.analyzeImage(
                    new AnalyzeImageRequestDto("original/" + input.hash(), threshold)
            );
            validateResponse(response);
            transactionTemplate.executeWithoutResult(status -> completeJob(jobId, response));
        } catch (Exception error) {
            markFailure(jobId, error);
        }
    }

    void completeJob(Long jobId, AnalyzeImageResponseDto response) {
        AiJob job = aiJobRepository.findById(jobId).orElse(null);
        if (!owns(job)) {
            log.warn("忽略已失去租约的 AI 任务结果 jobId={}", jobId);
            return;
        }

        Image image = job.getImage();
        image.setEmbedding(response.getEmbedding().stream().mapToDouble(Double::doubleValue).toArray());
        Set<Long> existingTagIds = image.getTagRelations().stream()
                .map(relation -> relation.getTag().getId())
                .collect(Collectors.toSet());
        for (Map.Entry<String, Double> entry : response.getTags().entrySet()) {
            try {
                Tag tag = tagService.getTagByName(entry.getKey());
                if (existingTagIds.add(tag.getId())) {
                    image.addTag(tag, entry.getValue());
                }
            } catch (RuntimeException ignored) {
                log.debug("跳过未知标签: {}", entry.getKey());
            }
        }

        Instant now = Instant.now();
        image.setAiStatus(AiJobService.IMAGE_READY);
        image.setAiError(null);
        image.setAiCompletedAt(now);
        job.setStatus(AiJobStatus.COMPLETED);
        job.setErrorMessage(null);
        job.setLockedBy(null);
        job.setLockedUntil(null);
        job.setUpdatedAt(now);
        job.setCompletedAt(now);
        imageRepository.save(image);
        aiJobRepository.save(job);
    }

    void markFailure(Long jobId, Exception error) {
        log.warn("AI 任务处理失败 jobId={}: {}", jobId, error.getMessage());
        transactionTemplate.executeWithoutResult(status -> {
            AiJob job = aiJobRepository.findById(jobId).orElse(null);
            if (!owns(job)) return;

            Instant now = Instant.now();
            Image image = job.getImage();
            String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            job.setErrorMessage(message);
            job.setLockedBy(null);
            job.setLockedUntil(null);
            job.setUpdatedAt(now);

            if (job.getAttempts() >= properties.getMaxAttempts()) {
                job.setStatus(AiJobStatus.FAILED);
                job.setCompletedAt(now);
                image.setAiStatus(AiJobService.IMAGE_FAILED);
                image.setAiError(message);
                image.setAiCompletedAt(now);
            } else {
                job.setStatus(AiJobStatus.PENDING);
                job.setNextRetryAt(now.plus(retryDelay(job.getAttempts())));
                image.setAiStatus(AiJobService.IMAGE_PENDING);
                image.setAiError(null);
                image.setAiCompletedAt(null);
            }
            imageRepository.save(image);
            aiJobRepository.save(job);
        });
    }

    private boolean owns(AiJob job) {
        return job != null
                && job.getStatus() == AiJobStatus.RUNNING
                && workerId.equals(job.getLockedBy());
    }

    Duration retryDelay(int attempts) {
        long multiplier = 1L << Math.min(Math.max(attempts - 1, 0), 20);
        Duration delay = properties.getRetryBaseDelay().multipliedBy(multiplier);
        return delay.compareTo(properties.getRetryMaxDelay()) > 0 ? properties.getRetryMaxDelay() : delay;
    }

    private void validateResponse(AnalyzeImageResponseDto response) {
        if (response == null || response.getEmbedding() == null || response.getEmbedding().size() != 512) {
            throw new IllegalStateException("AI 图片向量响应无效");
        }
        if (response.getTags() == null) {
            throw new IllegalStateException("AI 标签响应无效");
        }
    }

    private record ProcessingInput(String hash) {
    }
}
