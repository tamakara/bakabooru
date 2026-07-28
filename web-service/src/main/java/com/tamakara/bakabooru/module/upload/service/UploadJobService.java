package com.tamakara.bakabooru.module.upload.service;

import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.upload.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.upload.dto.UploadTaskDto;
import com.tamakara.bakabooru.module.upload.entity.UploadJob;
import com.tamakara.bakabooru.module.upload.entity.UploadJobStatus;
import com.tamakara.bakabooru.module.upload.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadJobService {

    private final UploadJobRepository uploadJobRepository;
    private final StorageService storageService;

    public void createTask(MultipartFile file) {
        UUID id = UUID.randomUUID();
        String stagingObjectName = "staging/" + id;
        String filename = FilenameUtils.getName(file.getOriginalFilename());
        if (filename == null || filename.isBlank()) {
            filename = id.toString();
        }

        try (InputStream inputStream = file.getInputStream()) {
            storageService.uploadStream(
                    stagingObjectName,
                    inputStream,
                    file.getSize(),
                    file.getContentType()
            );

            Instant now = Instant.now();
            UploadJob job = new UploadJob();
            job.setId(id);
            job.setFilename(filename);
            job.setStagingObjectName(stagingObjectName);
            job.setSize(file.getSize());
            job.setStatus(UploadJobStatus.PENDING);
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            uploadJobRepository.save(job);
        } catch (Exception e) {
            try {
                storageService.deleteFile(stagingObjectName);
            } catch (Exception ignored) {
                // Best-effort compensation for a partially uploaded staging object.
            }
            throw new RuntimeException("创建上传任务失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public TasksInfoDto getTasksInfo() {
        UploadTaskDto processingTask = uploadJobRepository
                .findFirstByStatusOrderByCreatedAtAsc(UploadJobStatus.PROCESSING)
                .map(UploadTaskDto::from)
                .orElse(null);
        List<UploadTaskDto> failedTasks = uploadJobRepository
                .findTop100ByStatusOrderByUpdatedAtDesc(UploadJobStatus.FAILED)
                .stream()
                .map(UploadTaskDto::from)
                .toList();
        return new TasksInfoDto(
                uploadJobRepository.countByStatus(UploadJobStatus.PENDING),
                processingTask,
                failedTasks
        );
    }

    @Transactional
    public void retryTask(UUID id) {
        UploadJob job = uploadJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("上传任务不存在"));
        if (job.getStatus() != UploadJobStatus.FAILED) {
            throw new RuntimeException("只有失败任务可以重试");
        }
        if (!storageService.existFile(job.getStagingObjectName())) {
            throw new RuntimeException("暂存文件不存在，请重新上传");
        }

        job.setStatus(UploadJobStatus.PENDING);
        job.setErrorMessage(null);
        job.setLockedBy(null);
        job.setLockedUntil(null);
        job.setUpdatedAt(Instant.now());
        uploadJobRepository.save(job);
    }

    public void clearFailedTasks() {
        List<UploadJob> failedJobs = uploadJobRepository
                .findByStatusOrderByUpdatedAtDesc(UploadJobStatus.FAILED);
        for (UploadJob job : failedJobs) {
            storageService.deleteFile(job.getStagingObjectName());
            uploadJobRepository.deleteById(job.getId());
        }
    }
}
