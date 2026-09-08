package com.tamakara.bakabooru.module.upload.service;

import com.tamakara.bakabooru.config.UploadProperties;
import com.tamakara.bakabooru.module.ai.service.AiJobService;
import com.tamakara.bakabooru.module.gallery.model.ImageInfo;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.image.service.ThumbnailService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.upload.entity.UploadJob;
import com.tamakara.bakabooru.module.upload.entity.UploadJobStatus;
import com.tamakara.bakabooru.module.upload.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadJobWorker {

    private final UploadJobRepository uploadJobRepository;
    private final StorageService storageService;
    private final ImageService imageService;
    private final ThumbnailService thumbnailService;
    private final AiJobService aiJobService;
    private final UploadProperties uploadProperties;
    private final SystemSettingService systemSettingService;
    private final TransactionTemplate transactionTemplate;

    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(
            fixedDelayString = "${app.upload.poll-interval-ms:1000}",
            initialDelayString = "${app.upload.initial-delay-ms:2000}"
    )
    public void processAvailableJobs() {
        UUID jobId;
        while ((jobId = claimNextJob()) != null) {
            processJob(jobId);
        }
    }

    @Scheduled(fixedRateString = "${app.upload.heartbeat-interval-ms:30000}")
    public void extendActiveLocks() {
        Instant now = Instant.now();
        transactionTemplate.executeWithoutResult(status -> uploadJobRepository.extendWorkerLocks(
                workerId,
                UploadJobStatus.PROCESSING,
                now.plus(uploadProperties.getLockDuration()),
                now
        ));
    }

    @Scheduled(cron = "${app.upload.cleanup-cron:0 0 3 * * *}")
    public void cleanupCompletedJobs() {
        Instant cutoff = Instant.now().minus(
                Duration.ofDays(systemSettingService.getUploadCompletedRetentionDays()));
        List<UploadJob> jobs = uploadJobRepository
                .findByStatusAndCompletedAtBefore(UploadJobStatus.COMPLETED, cutoff);
        for (UploadJob job : jobs) {
            try {
                storageService.deleteFile(job.getStagingObjectName());
                uploadJobRepository.deleteById(job.getId());
            } catch (Exception e) {
                log.warn("濠电偞鎸搁幊鎰板箖婵犲偆鍟呴柟缁樺笧閺嗘岸鏌熺€涙ê濮冪紒妤€鍊圭€靛ジ鎮╂潏銊ュ簥闂佸憡鏌￠埀顒傚仺娴滃ジ鎮?jobId={}: {}", job.getId(), e.getMessage());
            }
        }
    }

    private UUID claimNextJob() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            return uploadJobRepository.findNextClaimable(now)
                    .map(job -> {
                        job.setStatus(UploadJobStatus.PROCESSING);
                        job.setAttempts(job.getAttempts() + 1);
                        job.setErrorMessage(null);
                        job.setLockedBy(workerId);
                        job.setLockedUntil(now.plus(uploadProperties.getLockDuration()));
                        job.setUpdatedAt(now);
                        uploadJobRepository.saveAndFlush(job);
                        return job.getId();
                    })
                    .orElse(null);
        });
    }

    private void processJob(UUID jobId) {
        UploadJob job = uploadJobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        File stagingFile = null;
        try {
            stagingFile = storageService.getFile(job.getStagingObjectName());
            String hash = calculateHash(stagingFile);
            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("闂佹悶鍎辨晶鑺ユ櫠閺嵮屽晠闁圭粯甯為幗鐘绘煕?(Hash: " + hash + ")");
            }

            ImageInfo imageInfo = new ImageInfo(stagingFile);
            if (imageInfo.isAnimated()) {
                throw new UnsupportedOperationException("闂佸搫妫楅崐椋庣箔婢舵劕缁╂い鏍ㄧ☉閻︻噣鏌涢弬璇插婵?");
            }

            storageService.copyFile(job.getStagingObjectName(), "original/" + hash);
            thumbnailService.generateAndUploadThumbnail(stagingFile, hash);

            transactionTemplate.execute(status -> completeJob(jobId, job, imageInfo, hash));
        } catch (Exception e) {
            markFailed(jobId, e);
            return;
        } finally {
            if (stagingFile != null && stagingFile.exists() && !stagingFile.delete()) {
                log.warn("闂佸搫鍟版慨鐢垫兜閸洖绀嗛柣妯肩帛閻濈喎鈽夐幘绛规缂佽精鍩栫粋鎺旀嫚閹绘帩娼虫繛鎴炴尭閻°劌顪冮崒鐐叉闁搞儻闄勯? {}", stagingFile);
            }
        }

        try {
            storageService.deleteFile(job.getStagingObjectName());
        } catch (Exception e) {
            log.warn("闂佹悶鍎辨晶鑺ユ櫠閺嵮屽晠闁圭粯甯掑鎶藉箹鐎涙ɑ鎯堢紒杈ㄧ箖閹?staging 闁诲海鏁搁、濠囨寘閸曨偁鈧帡宕ㄩ娑樷偓鐐差熆閹壆绨块悷?jobId={}: {}", jobId, e.getMessage());
        }

    }

    private Void completeJob(UUID jobId, UploadJob snapshot, ImageInfo info, String hash) {
        Image image = new Image();
        image.setTitle(FilenameUtils.getBaseName(snapshot.getFilename()));
        image.setFileName(snapshot.getFilename());
        image.setExtension(info.getExtension());
        image.setSize(snapshot.getSize());
        image.setWidth(info.getWidth());
        image.setHeight(info.getHeight());
        image.setHash(hash);
        boolean hasAiSelection = (snapshot.getTagModelId() != null && !snapshot.getTagModelId().isBlank())
                || (snapshot.getVectorModelIds() != null && !snapshot.getVectorModelIds().isBlank());
        image.setStatus("NORMAL");
        Image savedImage = imageService.addImage(image);
        if (hasAiSelection) {
            aiJobService.enqueue(savedImage, snapshot.getTagModelId(), snapshot.getVectorModelIds());
        }

        UploadJob current = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + jobId));
        Instant now = Instant.now();
        current.setStatus(UploadJobStatus.COMPLETED);
        current.setImageId(savedImage.getId());
        current.setErrorMessage(null);
        current.setLockedBy(null);
        current.setLockedUntil(null);
        current.setUpdatedAt(now);
        current.setCompletedAt(now);
        uploadJobRepository.save(current);
        return null;
    }

    private void markFailed(UUID jobId, Exception error) {
        log.warn("婵炴垶鎸搁敃锝囨閼哥數顩烽悹鍥ㄥ絻椤倕顭跨捄鍝勵伀闁诡喖锕ュ鍕綇椤愩儛?jobId={}: {}", jobId, error.getMessage(), error);
        transactionTemplate.executeWithoutResult(status -> uploadJobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() == UploadJobStatus.COMPLETED) return;
            job.setStatus(UploadJobStatus.FAILED);
            job.setErrorMessage(error.getMessage());
            job.setLockedBy(null);
            job.setLockedUntil(null);
            job.setUpdatedAt(Instant.now());
            uploadJobRepository.save(job);
        }));
    }

    private String calculateHash(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return DigestUtils.sha256Hex(stream);
        } catch (Exception e) {
            throw new RuntimeException("闁荤姳绶ょ槐鏇㈡偩婵犳艾浼犻柛顐ｇ箘閻熸牕顭块幆鎵翱閻?, e");
        }
    }
}

