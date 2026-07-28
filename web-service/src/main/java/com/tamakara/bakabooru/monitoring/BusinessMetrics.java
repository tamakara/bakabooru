package com.tamakara.bakabooru.monitoring;

import com.tamakara.bakabooru.module.ai.entity.AiJobStatus;
import com.tamakara.bakabooru.module.ai.repository.AiJobRepository;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.upload.entity.UploadJobStatus;
import com.tamakara.bakabooru.module.upload.repository.UploadJobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final Counter uploadBytes;
    private final DistributionSummary aiAttempts;

    public BusinessMetrics(MeterRegistry registry, UploadJobRepository uploadJobs,
                           AiJobRepository aiJobs, ImageRepository images) {
        this.registry = registry;
        this.uploadBytes = Counter.builder("bakabooru.upload.bytes").register(registry);
        this.aiAttempts = DistributionSummary.builder("bakabooru.ai.job.attempts")
                .publishPercentileHistogram().register(registry);

        for (UploadJobStatus status : UploadJobStatus.values()) {
            Gauge.builder("bakabooru.upload.jobs", uploadJobs, repo -> repo.countByStatus(status))
                    .tag("status", label(status)).register(registry);
        }
        for (AiJobStatus status : AiJobStatus.values()) {
            Gauge.builder("bakabooru.ai.jobs", aiJobs, repo -> repo.countByStatus(status))
                    .tag("status", label(status)).register(registry);
        }
        Gauge.builder("bakabooru.library.images", images, ImageRepository::count).register(registry);
        Gauge.builder("bakabooru.library.bytes", images, ImageRepository::sumImageSize).register(registry);
        for (String status : new String[]{"PENDING", "PROCESSING", "READY", "FAILED"}) {
            Gauge.builder("bakabooru.library.images.by.ai.status", images,
                            repo -> repo.countByAiStatus(status))
                    .tag("status", status.toLowerCase(Locale.ROOT)).register(registry);
        }
    }

    public void uploadAccepted(long bytes) {
        uploadBytes.increment(bytes);
    }

    public void uploadProcessed(String result, Duration duration) {
        Counter.builder("bakabooru.upload.jobs.processed").tag("result", result).register(registry).increment();
        Timer.builder("bakabooru.upload.job.duration").tag("result", result)
                .publishPercentileHistogram().register(registry).record(duration);
    }

    public void aiProcessed(String result, int attempts, Duration duration) {
        Counter.builder("bakabooru.ai.jobs.processed").tag("result", result).register(registry).increment();
        Timer.builder("bakabooru.ai.job.duration").tag("result", result)
                .publishPercentileHistogram().register(registry).record(duration);
        aiAttempts.record(attempts);
    }

    public void search(String type, String result, long elapsedNanos) {
        Counter.builder("bakabooru.search.requests").tags("type", type, "result", result)
                .register(registry).increment();
        Timer.builder("bakabooru.search.duration").tags("type", type, "result", result)
                .publishPercentileHistogram().register(registry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
