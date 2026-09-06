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

    public void createTask(MultipartFile file, String tagModelId, String vectorModelIds) {
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
            job.setTagModelId(tagModelId);
            job.setVectorModelIds(vectorModelIds);
            uploadJobRepository.save(job);
        } catch (Exception e) {
            try {
                storageService.deleteFile(stagingObjectName);
            } catch (Exception ignored) {
                // Best-effort compensation for a partially uploaded staging object.
            }
            throw new RuntimeException("闂佸憡甯楃粙鎴犵磽閹惧鈻斿┑鐘辫兌閻愬﹤霉閻樹警鍤欏┑顔惧枑瀵板嫭娼忛銉? " + e.getMessage(), e);
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
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + id));
        if (job.getStatus() != UploadJobStatus.FAILED) {
            throw new RuntimeException("闂佸憡鐟禍婵嗭耿娴ｇ懓绶為弶鍫亯琚濇繛瀵稿Ь椤曆勬叏閻旂厧鐭楁い鏍ㄧ懁缁ㄤ即姊洪幓鎺斝ラ柣?");
        }
        if (!storageService.existFile(job.getStagingObjectName())) {
            throw new RuntimeException("闂佸搫妫楅崐鎼佹偤閵娾晛妫橀柛銉檮椤愯棄鈽夐幘宕囆㈤柣掳鍔戝畷鐑解€﹂幒鏃傤槷闁荤姴娲ㄩ崗姗€宕抽幖浣告濡鑳堕悷鎰?");
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
