package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.gallery.model.ImageInfo;
import com.tamakara.bakabooru.module.gallery.model.UploadTask;
import com.tamakara.bakabooru.module.gallery.model.UploadTaskQueue;
import com.tamakara.bakabooru.module.gallery.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.storage.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.entity.ImageTagRelation;
import com.tamakara.bakabooru.module.tag.service.TagService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
    private final SystemSettingService systemSettingService;

    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    private final TasksInfoDto tasksInfoDto = new TasksInfoDto();

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

            tasksInfoDto.setPendingCount(uploadTaskQueue.getPendingCount());
            tasksInfoDto.setProcessingTask(task);
            tasksInfoDto.setFailedTasks(uploadTaskQueue.getFailedTasks());

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

        try {
            File tempFile = File.createTempFile(taskId, null);
            file.transferTo(tempFile);
            task.setTempFilePath(tempFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("保存临时文件失败: " + e.getMessage(), e);
        }

        uploadTaskQueue.addTask(task);
    }

    private void processTask(UploadTask task) {
        String objectName = "temp/" + task.getId();

        String taskId = task.getId();
        long size = task.getSize();
        String filename = task.getFilename();
        String title = FilenameUtils.getBaseName(filename);

        File tempFile = new File(task.getTempFilePath());

        try {
            storageService.uploadFile("temp/" + taskId, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }

        try {
            String hash;
            try (InputStream stream = new FileInputStream(tempFile)) {
                hash = DigestUtils.sha256Hex(stream);
            } catch (Exception e) {
                throw new RuntimeException("计算哈希值失败: " + e.getMessage(), e);
            }

            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("图片已存在");
            }

            ImageInfo imageInfo = new ImageInfo(tempFile);

            if (imageInfo.isAnimated()) {
                throw new RuntimeException("暂不支持动图");
            }

            Set<ImageTagRelation> tagRelations = new HashSet<>();
            try {
                double threshold = systemSettingService.getDoubleSetting("tag.threshold");
                Map<String, Double> tags = tagService.tagImage(objectName, threshold);
                for (String name : tags.keySet()) {
                    ImageTagRelation relation = new ImageTagRelation();
                    relation.setScore(tags.get(name));
                    relation.setTag(tagService.getTagByName(name));
                    tagRelations.add(relation);
                }
            } catch (Exception e) {
                throw new RuntimeException("标签生成失败: " + e.getMessage(), e);
            }

            Image image = new Image();
            image.setTitle(title);
            image.setFileName(filename);
            image.setExtension(imageInfo.getExtension());
            image.setSize(size);
            image.setWidth(imageInfo.getWidth());
            image.setHeight(imageInfo.getHeight());
            image.setHash(hash);
            image.setTagRelations(tagRelations);

            imageService.addImage(image);
            storageService.copyFile(objectName, "original/" + hash);
        } catch (Exception e) {
            throw new RuntimeException("图片信息获取失败: " + e.getMessage(), e);
        } finally {
            storageService.deleteFile(objectName);
            tempFile.delete();
        }
    }


    // --- 对外暴露的管理接口 (供 Controller 调用) ---

    /**
     * 获取待处理数量
     */
    public TasksInfoDto getTasksInfo() {
        return tasksInfoDto;
    }

    /**
     * 重试任务
     */
    public void retryTask(String id) {
        uploadTaskQueue.retryTask(id);
    }

    /**
     * 清空失败任务
     */
    public void clearFailedTasks() {
        uploadTaskQueue.clearFailedTasks();
    }
}