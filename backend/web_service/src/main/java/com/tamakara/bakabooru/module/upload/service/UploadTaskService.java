package com.tamakara.bakabooru.module.upload.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.jpeg.JpegDirectory;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.storage.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.repository.TagRepository;
import com.tamakara.bakabooru.module.tag.service.TagService;
import com.tamakara.bakabooru.module.upload.model.UploadTask;
import com.tamakara.bakabooru.module.upload.model.UploadTaskQueue; // 假设你的Store/Queue类名叫这个
import com.tamakara.bakabooru.utils.FileUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class UploadTaskService {

    private final UploadTaskQueue uploadTaskQueue;
    private final StorageService storageService;
    private final TagService tagService;
    private final ImageService imageService;
    private final TagRepository tagRepository;
    private final SystemSettingService systemSettingService;

    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();


    @PostConstruct
    public void initTaskProcessor() {
        taskExecutor.submit(this::taskProcessingLoop);
    }

    private void taskProcessingLoop() {
        while (true) {
            String taskId = uploadTaskQueue.takeTask(5);
            if (taskId == null) continue;

            UploadTask task = uploadTaskQueue.getTask(taskId);
            if (task == null) continue;

            try {
                processTask(task);
                uploadTaskQueue.removeTaskData(taskId);
            } catch (Exception e) {
                uploadTaskQueue.moveToFailed(taskId, e.getMessage());
            }
        }
    }

    public void createTask(MultipartFile file) {
        String taskId = UUID.randomUUID().toString();

        UploadTask task = new UploadTask();
        task.setId(taskId);
        task.setFilename(file.getOriginalFilename());
        task.setSize(file.getSize());

        storageService.uploadFile("temp/" + taskId, file);
        uploadTaskQueue.addTask(task);
    }

    private void processTask(UploadTask task) {
        String objectName = "temp/" + task.getId();
        try (InputStream stream = storageService.getFileStream(objectName)) {
            String hash = DigestUtils.sha256Hex(stream);

            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("图片已存在");
            }

            try {
                // 生成标签
                double threshold = systemSettingService.getDoubleSetting("tag.threshold");
                Map<String, List<String>> tagData = tagService.tagImage(objectName, threshold);

                Set<Tag> tags = new HashSet<>();
                for (String tagType : tagData.keySet()) {
                    for (String tagName : tagData.get(tagType)) {
                        Tag tag = tagService.addTag(tagName, tagType);
                        tags.add(tag);
                    }
                }

                Image image = new Image();
                image.setTitle();
                image.setFileName(task.getFilename());
                image.setExtension(FileUtils.getExtension(task.getFilename()));
                image.setSize(task.getSize());
                image.setHash(hash);
                image.setTags(tags);
                image.setCreatedAt(LocalDateTime.now());
                image.setUpdatedAt(LocalDateTime.now());
            } catch (Exception e) {
                throw new RuntimeException("标签生成失败: " + e.getMessage(), e);
            }


        } catch (Exception e) {
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        }



        // 读取图片元数据 (宽/高)
        try {
            // 下载临时文件到本地进行分析
            File tempFile = storageService.downloadPendingFileToLocal(taskId);
            Metadata metadata = ImageMetadataReader.readMetadata(tempFile);
            JpegDirectory directory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (directory != null) {
                image.setWidth(directory.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
                image.setHeight(directory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
            }
            // 用完删掉本地临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (Exception e) {
            log.warn("读取元数据失败，使用默认值", e);
        }

        // 5. 将文件从 MinIO 临时桶移动到正式桶 (temp/uuid -> ab/hash.jpg)
        storageService.storeImage(taskId, hash);

        // 6. 写入数据库
        imageRepository.save(image);

        // 7. 标记内存对象状态 (虽然马上就要删除了，但保持一致性)
        task.setStatus(UploadTask.UploadStatus.COMPLETED);
    }

    // --- 对外暴露的管理接口 (供 Controller 调用) ---

    /**
     * 获取待处理数量
     */
    public long getPendingCount() {
        return uploadTaskQueue.getPendingCount();
    }

    /**
     * 获取失败任务列表
     */
    public List<UploadTask> getFailedTasks() {
        return uploadTaskQueue.getFailedTasks();
    }

    /**
     * 重试任务
     */
    public void retryTask(String id) {
        log.info("正在重试任务: {}", id);
        uploadTaskQueue.retryTask(id);
    }

    /**
     * 删除失败任务
     */
    public void deleteFailedTask(String id) {
        log.info("删除失败任务: {}", id);
        uploadTaskQueue.deleteFailedTask(id);
        // 同时清理 MinIO 里的临时文件
        storageService.deletePendingImage(id);
    }
}