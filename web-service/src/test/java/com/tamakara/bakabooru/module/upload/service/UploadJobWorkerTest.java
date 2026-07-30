package com.tamakara.bakabooru.module.upload.service;

import com.tamakara.bakabooru.config.UploadProperties;
import com.tamakara.bakabooru.module.ai.service.AiJobService;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.image.service.ThumbnailService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.upload.entity.UploadJobStatus;
import com.tamakara.bakabooru.module.upload.repository.UploadJobRepository;
import com.tamakara.bakabooru.monitoring.BusinessMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadJobWorkerTest {

    @Mock private UploadJobRepository uploadJobRepository;
    @Mock private StorageService storageService;
    @Mock private ImageService imageService;
    @Mock private ThumbnailService thumbnailService;
    @Mock private AiJobService aiJobService;
    @Mock private SystemSettingService systemSettingService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private BusinessMetrics metrics;

    @Test
    void cleanupUsesLatestRetentionSetting() {
        when(systemSettingService.getUploadCompletedRetentionDays()).thenReturn(14L);
        when(uploadJobRepository.findByStatusAndCompletedAtBefore(
                org.mockito.ArgumentMatchers.eq(UploadJobStatus.COMPLETED),
                org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(Collections.emptyList());
        UploadJobWorker worker = new UploadJobWorker(
                uploadJobRepository,
                storageService,
                imageService,
                thumbnailService,
                aiJobService,
                new UploadProperties(),
                systemSettingService,
                transactionTemplate,
                metrics
        );

        Instant before = Instant.now().minus(Duration.ofDays(14));
        worker.cleanupCompletedJobs();
        Instant after = Instant.now().minus(Duration.ofDays(14));

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(uploadJobRepository).findByStatusAndCompletedAtBefore(
                org.mockito.ArgumentMatchers.eq(UploadJobStatus.COMPLETED), cutoff.capture());
        assertThat(cutoff.getValue()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }
}
