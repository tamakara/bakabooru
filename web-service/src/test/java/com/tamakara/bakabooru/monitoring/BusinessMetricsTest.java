package com.tamakara.bakabooru.monitoring;

import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.upload.repository.UploadJobRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BusinessMetricsTest {

    @Test
    void recordsLowCardinalityBusinessEvents() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(
                registry, mock(UploadJobRepository.class),
                mock(AiJobRepository.class), mock(ImageRepository.class));

        metrics.uploadAccepted(1024);
        metrics.uploadProcessed("success", Duration.ofSeconds(2));
        metrics.aiProcessed("retry", 2, Duration.ofSeconds(3));
        metrics.search("semantic", "success", Duration.ofMillis(50).toNanos());

        assertThat(registry.get("bakabooru.upload.bytes").counter().count()).isEqualTo(1024);
        assertThat(registry.get("bakabooru.upload.jobs.processed").tag("result", "success").counter().count()).isEqualTo(1);
        assertThat(registry.get("bakabooru.ai.jobs.processed").tag("result", "retry").counter().count()).isEqualTo(1);
        assertThat(registry.get("bakabooru.search.requests").tags("type", "semantic", "result", "success").counter().count()).isEqualTo(1);
    }
}
