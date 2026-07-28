package com.tamakara.bakabooru.module.ai.service;

import com.tamakara.bakabooru.module.ai.entity.AiJob;
import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiJobServiceTest {

    @Mock
    private AiJobRepository aiJobRepository;
    @Mock
    private ImageRepository imageRepository;
    @InjectMocks
    private AiJobService service;

    @Test
    void enqueueCreatesOnePendingJob() {
        Image image = image(1L, AiJobService.IMAGE_PENDING);
        when(aiJobRepository.findByImageId(1L)).thenReturn(Optional.empty());
        when(aiJobRepository.save(any(AiJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiJob job = service.enqueue(image);

        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PENDING);
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getNextRetryAt()).isNotNull();
        verify(aiJobRepository).save(job);
    }

    @Test
    void retryResetsFailedJob() {
        Image image = image(1L, AiJobService.IMAGE_FAILED);
        image.setAiError("failed");
        AiJob job = new AiJob();
        job.setImage(image);
        job.setStatus(AiJobStatus.FAILED);
        job.setAttempts(5);
        job.setLockedBy("old-worker");
        job.setLockedUntil(Instant.now());
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(aiJobRepository.findByImageId(1L)).thenReturn(Optional.of(job));
        when(imageRepository.save(image)).thenReturn(image);

        Image result = service.retry(1L);

        assertThat(result.getAiStatus()).isEqualTo(AiJobService.IMAGE_PENDING);
        assertThat(result.getAiError()).isNull();
        assertThat(job.getStatus()).isEqualTo(AiJobStatus.PENDING);
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getLockedBy()).isNull();
    }

    @Test
    void retryRejectsNonFailedImage() {
        Image image = image(1L, AiJobService.IMAGE_PENDING);
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> service.retry(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    private Image image(Long id, String status) {
        Image image = new Image();
        image.setId(id);
        image.setAiStatus(status);
        return image;
    }
}
