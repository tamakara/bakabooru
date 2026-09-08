package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.config.AiJobProperties;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageRequestDto;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageResponseDto;
import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.entity.ImageEmbedding;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.image.service.StorageService;
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
import java.util.Arrays;
import java.util.List;
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
    private final StorageService storageService;
    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${app.ai-job.poll-interval-ms:1000}", initialDelayString = "${app.ai-job.initial-delay-ms:2000}")
    public void processAvailableJobs() { Long jobId; while ((jobId = claimNextJob()) != null) processJob(jobId); }

    @Scheduled(fixedRateString = "${app.ai-job.heartbeat-interval-ms:30000}")
    public void extendActiveLocks() {
        Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> aiJobRepository.extendWorkerLocks(workerId, AiJobStatus.RUNNING, now.plus(properties.getLockDuration()), now));
    }

    Long claimNextJob() {
        return transactionTemplate.execute(status -> aiJobRepository.findNextClaimable(Instant.now()).map(job -> {
            Instant now = Instant.now();
            job.setStatus(AiJobStatus.RUNNING); job.setAttempts(job.getAttempts() + 1); job.setLockedBy(workerId);
            job.setLockedUntil(now.plus(properties.getLockDuration())); job.setUpdatedAt(now);
            aiJobRepository.saveAndFlush(job);
            Image image = job.getImage(); image.setStatus("ANALYZING"); image.setAnalysisStage(activeStage(image.getId()));
            image.setAnalysisError(null); image.setAnalysisStartedAt(now); image.setAnalysisCompletedAt(null);
            if (job.getVectorModelIds() != null) for (String modelId : job.getVectorModelIds().split(",")) image.getIndexVectors().stream().filter(vector -> modelId.trim().equals(vector.getModelId())).forEach(vector -> vector.setStatus("PROCESSING"));
            imageRepository.save(image); return job.getId();
        }).orElse(null));
    }

    void processJob(Long jobId) {
        try {
            ProcessingInput input = transactionTemplate.execute(status -> aiJobRepository.findById(jobId)
                    .map(job -> new ProcessingInput(job.getImage().getHash(), job.getTagModelId(), job.getVectorModelIds(), job.getCapability()))
                    .orElseThrow(() -> new IllegalStateException("AI job not found: " + jobId)));
            if (!storageService.existFile("original/" + input.hash())) {
                transactionTemplate.executeWithoutResult(tx -> aiJobRepository.findById(jobId).ifPresent(job -> {
                    job.setStatus(AiJobStatus.FAILED);
                    job.setErrorMessage("Source file is missing");
                    job.setCompletedAt(Instant.now());
                    job.setLockedBy(null);
                    job.setLockedUntil(null);
                    job.getImage().setStatus("MISSING");
                    aiJobRepository.save(job);
                }));
                return;
            }
            double threshold = systemSettingService.getDoubleSetting(SystemSettingService.TAG_THRESHOLD);
            List<String> models = input.vectorModelIds() == null || input.vectorModelIds().isBlank() ? null : Arrays.stream(input.vectorModelIds().split(",")).map(String::trim).toList();
            AnalyzeImageRequestDto request = new AnalyzeImageRequestDto("original/" + input.hash(), threshold, input.tagModelId(), models);
            AnalyzeImageResponseDto response = switch (input.capability()) {
                case "TAGS" -> aiServiceClient.analyzeTags(request);
                case "VECTORS" -> aiServiceClient.analyzeVectors(request);
                default -> aiServiceClient.analyzeImage(request);
            };
            validateResponse(response, input.capability());
            transactionTemplate.executeWithoutResult(status -> completeJob(jobId, response));
        } catch (Exception error) { markFailure(jobId, error); }
    }

    void completeJob(Long jobId, AnalyzeImageResponseDto response) {
        AiJob job = aiJobRepository.findById(jobId).orElse(null); if (!owns(job)) return;
        Image image = job.getImage(); String selectedVectors = job.getVectorModelIds();
        if (selectedVectors != null && !selectedVectors.isBlank()) {
            Map<String, List<Double>> embeddings = response.getEmbeddings();
            for (String rawId : selectedVectors.split(",")) {
                String modelId = rawId.trim();
                List<Double> values = embeddings == null ? null : embeddings.get(modelId);
                if (values == null && response.getEmbedding() != null && modelId.equals(firstModelId(selectedVectors))) {
                    values = response.getEmbedding();
                }
                if (values == null) throw new IllegalStateException("AI vector response missing model " + modelId);
                double[] embedding = values.stream().mapToDouble(Double::doubleValue).toArray();
                image.getIndexVectors().removeIf(existing -> modelId.equals(existing.getModelId()));
                ImageEmbedding vector = new ImageEmbedding(); vector.setImage(image); vector.setModelId(modelId); vector.setModelRevision("1");
                vector.setEmbedding(embedding); vector.setStatus("READY"); vector.setComputedAt(Instant.now()); image.getIndexVectors().add(vector);
            }
        }
        boolean includesTags = "TAGS".equals(job.getCapability()) || "TAGS_AND_VECTORS".equals(job.getCapability());
        if (includesTags && response.getTags() != null) {
            image.getTagRelations().removeIf(relation -> "AI".equals(relation.getSourceType()));
            Set<Long> existingTagIds = image.getTagRelations().stream().map(relation -> relation.getTag().getId()).collect(Collectors.toSet());
            for (Map.Entry<String, Double> entry : response.getTags().entrySet()) {
                try { Tag tag = tagService.getTagByName(entry.getKey()); if (existingTagIds.add(tag.getId())) image.getTagRelations().add(new com.tamakara.bakabooru.module.tag.entity.ImageTagRelation(image, tag, entry.getValue(), "AI")); }
                catch (RuntimeException ignored) { log.debug("Unable to persist generated tag {}", entry.getKey()); }
            }
        }
        Instant now = Instant.now(); job.setStatus(AiJobStatus.COMPLETED); job.setErrorMessage(null); job.setLockedBy(null); job.setLockedUntil(null); job.setUpdatedAt(now); job.setCompletedAt(now);
        aiJobRepository.save(job); refreshImageStatus(image, now);
    }

    void markFailure(Long jobId, Exception error) {
        transactionTemplate.executeWithoutResult(status -> {
            AiJob job = aiJobRepository.findById(jobId).orElse(null); if (!owns(job)) return;
            Instant now = Instant.now(); String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            job.setErrorMessage(message); job.setLockedBy(null); job.setLockedUntil(null); job.setUpdatedAt(now);
            if (job.getAttempts() >= systemSettingService.getAiMaxAttempts()) { job.setStatus(AiJobStatus.FAILED); job.setCompletedAt(now); }
            else { job.setStatus(AiJobStatus.PENDING); job.setNextRetryAt(now.plus(retryDelay(job.getAttempts()))); }
            aiJobRepository.save(job); Image image = job.getImage(); image.setAnalysisError(message); image.setAnalysisCompletedAt(job.getStatus() == AiJobStatus.FAILED ? now : null);
            if (job.getStatus() == AiJobStatus.FAILED && job.getVectorModelIds() != null) for (String modelId : job.getVectorModelIds().split(",")) image.getIndexVectors().stream().filter(vector -> modelId.trim().equals(vector.getModelId())).forEach(vector -> { vector.setStatus("FAILED"); vector.setErrorMessage(message); });
            refreshImageStatus(image, now);
        });
    }

    private void refreshImageStatus(Image image, Instant now) {
        if (!storageService.existFile("original/" + image.getHash())) image.setStatus("MISSING");
        else if (aiJobRepository.existsByImageIdAndStatusIn(image.getId(), List.of(AiJobStatus.PENDING, AiJobStatus.RUNNING))) { image.setStatus("ANALYZING"); image.setAnalysisStage(activeStage(image.getId())); image.setAnalysisCompletedAt(null); }
        else {
            AiJob latest = aiJobRepository.findFirstByImageIdOrderByUpdatedAtDesc(image.getId()).orElse(null);
            boolean failed = latest != null && latest.getStatus() == AiJobStatus.FAILED;
            image.setStatus(failed ? "ERROR" : "NORMAL");
            image.setAnalysisStage(failed && latest != null ? latest.getCapability() : null);
            if (!failed) { image.setAnalysisError(null); image.setAnalysisCompletedAt(now); }
        }
        imageRepository.save(image);
    }

    private boolean owns(AiJob job) { return job != null && job.getStatus() == AiJobStatus.RUNNING && workerId.equals(job.getLockedBy()); }
    Duration retryDelay(int attempts) { long multiplier = 1L << Math.min(Math.max(attempts - 1, 0), 20); Duration delay = Duration.ofSeconds(systemSettingService.getAiRetryBaseDelaySeconds()).multipliedBy(multiplier); Duration max = Duration.ofSeconds(systemSettingService.getAiRetryMaxDelaySeconds()); return delay.compareTo(max) > 0 ? max : delay; }
    private void validateResponse(AnalyzeImageResponseDto response, String capability) {
        if (response == null) throw new IllegalStateException("AI response invalid");
        if (!"TAGS".equals(capability) && response.getEmbedding() == null && (response.getEmbeddings() == null || response.getEmbeddings().isEmpty())) {
            throw new IllegalStateException("AI vector response invalid");
        }
        if (!"VECTORS".equals(capability) && response.getTags() == null) throw new IllegalStateException("AI tag response invalid");
    }

    private String firstModelId(String modelIds) {
        return modelIds.split(",")[0].trim();
    }

    private String activeStage(Long imageId) {
        boolean tags = false;
        boolean vectors = false;
        for (AiJob job : aiJobRepository.findAllByImageId(imageId)) {
            if (job.getStatus() != AiJobStatus.PENDING && job.getStatus() != AiJobStatus.RUNNING) continue;
            tags |= "TAGS".equals(job.getCapability()) || "TAGS_AND_VECTORS".equals(job.getCapability());
            vectors |= "VECTORS".equals(job.getCapability()) || "TAGS_AND_VECTORS".equals(job.getCapability());
        }
        return tags && vectors ? "TAGS_AND_VECTORS" : tags ? "TAGS" : "VECTORS";
    }
    private record ProcessingInput(String hash, String tagModelId, String vectorModelIds, String capability) {}
}
