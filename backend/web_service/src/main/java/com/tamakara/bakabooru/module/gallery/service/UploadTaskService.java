package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.service.EmbeddingService;
import com.tamakara.bakabooru.module.gallery.model.ImageInfo;
import com.tamakara.bakabooru.module.gallery.model.UploadTask;
import com.tamakara.bakabooru.module.gallery.model.UploadTaskQueue;
import com.tamakara.bakabooru.module.gallery.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadTaskService {

    private final UploadTaskQueue uploadTaskQueue;
    private final StorageService storageService;
    private final TagService tagService;
    private final ImageService imageService;
    private final SystemSettingService systemSettingService;
    private final EmbeddingService embeddingService;

    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

    private UploadTask processingTask = null;

    @PostConstruct
    public void initTaskProcessor() {
        log.info("初始化上传任务处理器...");
        taskExecutor.submit(this::taskProcessingLoop);
    }

    private void taskProcessingLoop() {
        log.info("上传任务处理循环已启动");
        while (true) {
            try {
                String taskId = uploadTaskQueue.takeTask(5);
                if (taskId == null) continue;

                log.info("开始处理任务: {}", taskId);
                UploadTask task = uploadTaskQueue.getTask(taskId);
                if (task == null) {
                    log.warn("任务数据不存在: {}", taskId);
                    continue;
                }
                processingTask = task;

                try {
                    processTask(task);
                    uploadTaskQueue.removeTaskData(taskId);
                    log.info("任务处理完成: {}", taskId);
                } catch (Exception e) {
                    log.error("任务处理失败: {}, 错误: {}", taskId, e.getMessage(), e);
                    uploadTaskQueue.moveToFailed(taskId, e.getMessage());
                } finally {
                    processingTask = null;
                }
            } catch (Exception e) {
                log.error("任务处理循环异常: {}", e.getMessage(), e);
                // 发生异常后短暂休眠，避免异常时 CPU 空转
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
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
        String objectName = "temp/upload/" + task.getId();
        File tempFile = new File(task.getTempFilePath());

        try {
            // 1. 上传临时文件到 MinIO
            log.info("上传临时文件: {}", objectName);
            storageService.uploadFile(objectName, tempFile);

            // 2. 计算文件哈希
            String hash = calculateHash(tempFile);

            // 3. 查重
            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("图片已存在 (Hash: " + hash + ")");
            }

            // 4. 解析图片信息
            ImageInfo imageInfo = new ImageInfo(tempFile);
            if (imageInfo.isAnimated()) {
                throw new UnsupportedOperationException("暂不支持动图");
            }

            // 5. 生成标签 (AI)
            log.info("正在生成标签...");
            double threshold = systemSettingService.getDoubleSetting("tag.threshold");
            Map<String, Double> tagsMap = tagService.tagImage(objectName, threshold);

            // 6. 生成向量 (Embedding)
            log.info("正在生成向量...");
            double[] embedding = embeddingService.generateImageEmbedding(objectName);

            // 7. 保存元数据
            saveImageMetadata(task, imageInfo, hash, embedding, tagsMap);

            // 8. 归档文件
            log.info("归档文件到正式目录...");
            storageService.copyFile(objectName, "original/" + hash);

        } catch (Exception e) {
            throw new RuntimeException("处理图片失败: " + e.getMessage(), e);
        } finally {
            // 清理临时资源
            if (tempFile.exists()) tempFile.delete();
            try {
                storageService.deleteFile(objectName);
            } catch (Exception ignored) {
                log.warn("无法清理临时文件对象: {}", objectName);
            }
        }
    }

    private String calculateHash(File file) {
         try (InputStream stream = new FileInputStream(file)) {
            return DigestUtils.sha256Hex(stream);
        } catch (Exception e) {
            throw new RuntimeException("计算哈希失败", e);
        }
    }

    private void saveImageMetadata(UploadTask task, ImageInfo info, String hash, double[] embedding, Map<String, Double> tags) {
        Image image = new Image();
        image.setTitle(FilenameUtils.getBaseName(task.getFilename()));
        image.setFileName(task.getFilename());
        image.setExtension(info.getExtension());
        image.setSize(task.getSize());
        image.setWidth(info.getWidth());
        image.setHeight(info.getHeight());
        image.setHash(hash);
        image.setEmbedding(embedding);

        for (Map.Entry<String, Double> entry : tags.entrySet()) {
            Tag tag = tagService.getTagByName(entry.getKey());
            image.addTag(tag, entry.getValue());
        }

        imageService.addImage(image);
    }


    // --- 对外暴露的管理接口 (供 Controller 调用) ---

    /**
     * 获取待处理数量
     */
    public TasksInfoDto getTasksInfo() {
        // 构建新的响应对象，获取即时数据
        TasksInfoDto response = new TasksInfoDto();
        // processingTask 由处理循环维护，直接读取当前状态
        response.setProcessingTask(processingTask);

        // pendingCount 和 failedTasks 直接从队列获取，确保数据实时性
        // 特别是当处理循环处于空闲等待状态时，缓存的数据可能是旧的
        response.setPendingCount(uploadTaskQueue.getPendingCount());
        response.setFailedTasks(uploadTaskQueue.getFailedTasks());

        return response;
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