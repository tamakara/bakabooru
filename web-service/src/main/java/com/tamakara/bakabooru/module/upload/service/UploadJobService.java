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
            throw new RuntimeException("闁告帗绋戠紓鎾寸▔婵犱胶鐐婂ù鐘侯嚙婵喐寰勬潏顐バ? " + e.getMessage(), e);
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
                .orElseThrow(() -> new RuntimeException("濞戞挸锕ｇ槐鑸电鐠囨彃顫ゅ☉鎾崇Т閻°劑宕?)");
        if (job.getStatus() != UploadJobStatus.FAILED) {
            throw new RuntimeException("闁告瑯浜濆﹢浣瑰緞鏉堫偉袝濞寸姾顕ф慨鐔煎矗椤栨瑤绨伴梺鎻掔Х閻?");
        }
        if (!storageService.existFile(job.getStagingObjectName())) {
            throw new RuntimeException("闁哄棗鍊搁悺銊╁棘閸ワ附顐藉☉鎾崇Т閻°劑宕烽…鎺旂閻犲洨鍏橀崳鎼佸棘妫颁胶鐟愬ù?");
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
