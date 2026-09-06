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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiJobWorker {

    private static final String DEFAULT_VECTOR_MODEL = "clip-vit-base-patch32";
    private static final String DEFAULT_TAG_MODEL = "camie-tagger-v2";

    private final AiJobRepository aiJobRepository;
    private final ImageRepository imageRepository;
    private final AiServiceClient aiServiceClient;
    private final TagService tagService;
    private final SystemSettingService systemSettingService;
    private final AiJobProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final StorageService storageService;

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
                        image.setStatus("PROCESSING");
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
                    .map(job -> new ProcessingInput(job.getImage().getHash(), job.getTagModelId(), job.getVectorModelIds()))
                    .orElseThrow(() -> new IllegalStateException("AI job not found: " + jobId)));
            double threshold = systemSettingService.getDoubleSetting("tag.threshold");
            if (!storageService.existFile("original/" + input.hash())) {
                transactionTemplate.executeWithoutResult(tx -> aiJobRepository.findById(jobId).ifPresent(job -> { job.getImage().setStatus("MISSING"); imageRepository.save(job.getImage()); }));
                return;
            }
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
            log.warn("闂傚倸顭崑鍕洪妸鈺佺柧妞ゆ劧绠戝Ч鏌ユ煙闁箑澧婚柛鐔锋嚇閺岀喓绱掑Ο铏诡伝婵炲瓨绮岄妶鎼佸蓟濞戞鐔煎垂椤斿吋鍎俊鐐€ら崑渚€宕愬Δ鍛剦妞ゅ繐鐗婇弲婊堟煟閹伴潧澧伴柡?AI 婵犵數鍋涢顓熸叏妤ｅ喚鏁嬬憸搴ㄥ箞閵娾晜鍋勯柧蹇撴贡閿涙粓姊虹憴鍕姢妞ゆ洦鍘界粋?jobId={}", jobId);
            return;
        }

        Image image = job.getImage();
        image.setEmbedding(response.getEmbedding().stream().mapToDouble(Double::doubleValue).toArray());
        ImageEmbedding vector = new ImageEmbedding();
        vector.setImage(image);
        String selectedVectors = job.getVectorModelIds();
        String[] vectorModels = selectedVectors == null || selectedVectors.isBlank()
                ? new String[]{DEFAULT_VECTOR_MODEL}
                : selectedVectors.split(",");
        vector.setModelId(vectorModels[0].trim());
        vector.setModelRevision("1");
        vector.setEmbedding(image.getEmbedding());
        vector.setStatus("READY");
        vector.setComputedAt(Instant.now());
        image.getIndexVectors().removeIf(existing -> java.util.Arrays.asList(vectorModels).contains(existing.getModelId()));
        image.getIndexVectors().add(vector);
        for (int i = 1; i < vectorModels.length; i++) {
            ImageEmbedding extra = new ImageEmbedding();
            extra.setImage(image);
            extra.setModelId(vectorModels[i].trim());
            extra.setModelRevision("1");
            extra.setEmbedding(image.getEmbedding());
            extra.setStatus("READY");
            extra.setComputedAt(Instant.now());
            image.getIndexVectors().add(extra);
        }
        String tagModel = job.getTagModelId() == null || job.getTagModelId().isBlank()
                ? DEFAULT_TAG_MODEL : job.getTagModelId().trim();
        image.setTagModelId(tagModel);
        image.getTagRelations().removeIf(relation -> "AI".equals(relation.getSourceType()));
        Set<Long> existingTagIds = image.getTagRelations().stream()
                .map(relation -> relation.getTag().getId())
                .collect(Collectors.toSet());
        for (Map.Entry<String, Double> entry : response.getTags().entrySet()) {
            try {
                Tag tag = tagService.getTagByName(entry.getKey());
                if (existingTagIds.add(tag.getId())) {
                    image.getTagRelations().add(new com.tamakara.bakabooru.module.tag.entity.ImageTagRelation(
                            image, tag, entry.getValue(), "AI", tagModel));
                }
            } catch (RuntimeException ignored) {
                log.debug("闂備浇宕垫慨鎾箹椤愶附鍋柛銉㈡櫆瀹曟煡鏌涢幇闈涙灈閻庢艾顦伴妵鍕疀閹炬惌妫ら梺鍛婄憿閸嬫捇姊绘担鍛婃儓缂佸娼欑叅闁靛ň鏅╅弫? {}", entry.getKey());
            }
        }

        Instant now = Instant.now();
        image.setStatus("AVAILABLE");
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
        log.warn("AI 婵犵數鍋涢顓熸叏妤ｅ喚鏁嬬憸搴ㄥ箞閵娾晜鍋勭紒瀣硶缁愮偤姊洪崨濠冨闁告ü绮欏畷鎰版倷瀹割喚鍞甸梺璇″灡婢瑰棛鑺遍崸妤佸仭?jobId={}: {}", jobId, error.getMessage());
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

            if (job.getAttempts() >= systemSettingService.getAiMaxAttempts()) {
                job.setStatus(AiJobStatus.FAILED);
                job.setCompletedAt(now);
                image.setStatus("AVAILABLE");
                image.setAiError(message);
                image.setAiCompletedAt(now);
            } else {
                job.setStatus(AiJobStatus.PENDING);
                job.setNextRetryAt(now.plus(retryDelay(job.getAttempts())));
                image.setStatus("PROCESSING");
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
        Duration baseDelay = Duration.ofSeconds(systemSettingService.getAiRetryBaseDelaySeconds());
        Duration maxDelay = Duration.ofSeconds(systemSettingService.getAiRetryMaxDelaySeconds());
        Duration delay = baseDelay.multipliedBy(multiplier);
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }

    private void validateResponse(AnalyzeImageResponseDto response) {
        if (response == null || response.getEmbedding() == null || response.getEmbedding().size() != 512) {
            throw new IllegalStateException("AI response invalid");
        }
        if (response.getTags() == null) {
            throw new IllegalStateException("AI response invalid");
        }
    }

    private record ProcessingInput(String hash, String tagModelId, String vectorModelIds) {
    }
}
