package com.tamakara.bakabooru.module.upload.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.jpeg.JpegDirectory;
import com.tamakara.bakabooru.module.file.service.StorageService;
import com.tamakara.bakabooru.module.tag.service.TagService;
import com.tamakara.bakabooru.utils.FileUtils;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.repository.TagRepository;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.upload.model.UploadTask;
import com.tamakara.bakabooru.module.upload.model.UploadTaskStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadQueueService {

    private final StorageService storageService;
    private final TagService tagService;
    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final SystemSettingService systemSettingService;

    // 内存存储任务队列
    private final UploadTaskStore taskStore = new UploadTaskStore();

    public UploadTask createTask(MultipartFile file, Boolean enableTagging) {
        String taskId = UUID.randomUUID().toString();

        UploadTask task = new UploadTask();
        task.setId(taskId);
        task.setFilename(file.getOriginalFilename());
        task.setSize(file.getSize());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setHash(FileUtils.calculateHash(file));
        task.setEnableTagging(enableTagging != null && enableTagging);

        task.setStatus(UploadTask.UploadStatus.PENDING);
        taskStore.addTask(task);

        try {
            // 验证文件
            validateFile(file);
            // 临时存储文件
            storageService.storePendingImage(taskId, file);
            // 异步开始处理
            processTask(taskId);
            log.info("任务处理成功,taskId:" + taskId);
        } catch (Exception e) {
            log.error("任务处理失败,taskId:" + taskId, e);
            task.setStatus(UploadTask.UploadStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }

        return task;
    }

    private void validateFile(MultipartFile file) {
        // 检查大小
        long maxSize = systemSettingService.getLongSetting("upload.max-file-size", 52428800); // 默认 50MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("文件过大。最大允许: " + maxSize);
        }

        // 检查扩展名
        String allowedExtensions = systemSettingService.getSetting("upload.allowed-extensions", "jpg,png,webp,gif,jpeg");
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String ext = FileUtils.getExtension(filename).toLowerCase().replace(".", "");
            if (!Arrays.asList(allowedExtensions.split(",")).contains(ext)) {
                throw new RuntimeException("不支持的文件类型。允许的类型: " + allowedExtensions);
            }
        }
    }

    @Async("uploadExecutor")
    public void processTask(String taskId) {
        UploadTask task = taskStore.getTask(taskId);
        if (task == null) return;

        updateStatus(task, UploadTask.UploadStatus.PROCESSING);

        String hash = task.getHash();

        // 1. 检查重复
        Optional<Image> existingImage = imageRepository.findByHash(hash);
        if (existingImage.isPresent()) {
            throw new RuntimeException("重复的图片: " + existingImage.get().getId());
        }

        Set<Tag> tags = new HashSet<>();

        // 2. 生成标签
        if (task.isEnableTagging()) {
            updateStatus(task, UploadTask.UploadStatus.TAGGING);
            try {
                // 调用 AIService 获取标签
                Map<String, List<String>> tagData = tagService.tagImage("temp/pending/" + taskId);

                // 将 TagData 转换为实体
                for (String tagType : tagData.keySet()) {
                    for (String tagName : tagData.get(tagType)) {
                        Tag tag = tagRepository.findByName(tagName)
                                .orElseGet(() -> {
                                    Tag newTag = new Tag();
                                    newTag.setName(tagName);
                                    newTag.setType(tagType);
                                    return tagRepository.save(newTag);
                                });
                        tags.add(tag);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        // 3. 保存
        updateStatus(task, UploadTask.UploadStatus.SAVING);

        Image image = new Image();

        String originalFilename = task.getFilename();
        String title = originalFilename;
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex != -1) {
            title = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex + 1);
        }

        image.setTitle(title);
        image.setFileName(originalFilename);
        image.setExtension(extension);

        image.setSize(task.getSize());
        image.setHash(hash);
        image.setTags(tags);
        image.setCreatedAt(LocalDateTime.now());
        image.setUpdatedAt(LocalDateTime.now());

        // 获取尺寸
        Path filePath = storageService.getPendingImagePath(taskId);
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(filePath.toFile());
            JpegDirectory directory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (directory != null) {
                image.setWidth(directory.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
                image.setHeight(directory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
            }
        } catch (Exception e) {
            throw new RuntimeException("无法获取图片元数据", e);
        }

        storageService.storeImage(taskId, hash);
        imageRepository.save(image);

        updateStatus(task, UploadTask.UploadStatus.COMPLETED);
    }

    private void updateStatus(UploadTask task, UploadTask.UploadStatus status) {
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
    }


    public List<UploadTask> getAllTasks() {
        return taskStore.getAllTasks();
    }

    public UploadTask getTask(String id) {
        return taskStore.getTask(id);
    }

    public void deleteTask(String id) {
        taskStore.removeTask(id);
    }

    public void clearTasks() {
        taskStore.clearCompletedOrFailed();
    }
}
