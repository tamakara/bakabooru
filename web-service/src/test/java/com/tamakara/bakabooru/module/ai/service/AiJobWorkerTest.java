package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.config.AiJobProperties;
import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.ai.dto.AnalyzeImageResponseDto;
import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiJobWorkerTest {

    @Mock private AiJobRepository aiJobRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private TagService tagService;
    @Mock private SystemSettingService systemSettingService;
    @Mock private TransactionTemplate transactionTemplate;

    private AiJobProperties properties;
    private AiJobWorker worker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties = new AiJobProperties();
        properties.setMaxAttempts(5);
        properties.setRetryBaseDelay(Duration.ofSeconds(30));
        properties.setRetryMaxDelay(Duration.ofMinutes(30));
        worker = new AiJobWorker(aiJobRepository, imageRepository, aiServiceClient, tagService,
                systemSettingService, properties, transactionTemplate);
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void retryDelayUsesExponentialBackoff() {
        assertThat(worker.retryDelay(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(worker.retryDelay(2)).isEqualTo(Duration.ofMinutes(1));
        assertThat(worker.retryDelay(4)).isEqualTo(Duration.ofMinutes(4));
    }

    @Test
    void finalFailureMarksJobAndImageFailed() {
        AiJob job = runningJob(5, workerId());
        when(aiJobRepository.findById(1L)).thenReturn(Optional.of(job));

        worker.markFailure(1L, new RuntimeException("inference failed"));

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.FAILED);
        assertThat(job.getImage().getAiStatus()).isEqualTo(AiJobService.IMAGE_FAILED);
        assertThat(job.getImage().getAiError()).isEqualTo("inference failed");
    }

    @Test
    void transientFailureReturnsJobToPending() {
        AiJob job = runningJob(2, workerId());
        when(aiJobRepository.findById(1L)).thenReturn(Optional.of(job));

        worker.markFailure(1L, new RuntimeException("temporary"));

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PENDING);
        assertThat(job.getNextRetryAt()).isAfter(Instant.now());
        assertThat(job.getImage().getAiStatus()).isEqualTo(AiJobService.IMAGE_PENDING);
    }

    @Test
    void staleWorkerCannotCommitResult() {
        AiJob job = runningJob(1, "another-worker");
        when(aiJobRepository.findById(1L)).thenReturn(Optional.of(job));
        AnalyzeImageResponseDto response = new AnalyzeImageResponseDto();
        response.setTags(Collections.emptyMap());
        response.setEmbedding(Collections.nCopies(512, 0.0));

        worker.completeJob(1L, response);

        verify(imageRepository, never()).save(any());
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.RUNNING);
    }

    private AiJob runningJob(int attempts, String lockedBy) {
        Image image = new Image();
        image.setId(10L);
        image.setAiStatus(AiJobService.IMAGE_PROCESSING);
        AiJob job = new AiJob();
        job.setId(1L);
        job.setImage(image);
        job.setStatus(AiJobStatus.RUNNING);
        job.setAttempts(attempts);
        job.setLockedBy(lockedBy);
        return job;
    }

    private String workerId() {
        return (String) ReflectionTestUtils.getField(worker, "workerId");
    }
}
