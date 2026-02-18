package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.service.EmbeddingService;
import com.tamakara.bakabooru.module.gallery.model.ImageInfo;
import com.tamakara.bakabooru.module.gallery.model.UploadTask;
import com.tamakara.bakabooru.module.gallery.model.UploadTaskQueue;
import com.tamakara.bakabooru.module.gallery.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.storage.service.StorageService;
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
        String objectName = "temp/" + task.getId();

        long size = task.getSize();
        String filename = task.getFilename();
        String title = FilenameUtils.getBaseName(filename);

        File tempFile = new File(task.getTempFilePath());

        // 1. 上传临时文件到 MinIO
        log.info("上传临时文件到 MinIO: {}", objectName);
        try {
            storageService.uploadFile(objectName, tempFile);
        } catch (Exception e) {
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }

        try {
            // 2. 计算文件哈希
            log.info("计算文件哈希...");
            String hash;
            try (InputStream stream = new FileInputStream(tempFile)) {
                hash = DigestUtils.sha256Hex(stream);
            } catch (Exception e) {
                throw new RuntimeException("计算哈希值失败: " + e.getMessage(), e);
            }

            // 3. 检查图片是否已存在
            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("图片已存在");
            }

            // 4. 获取图片信息（尺寸、格式等）
            log.info("获取图片信息...");
            ImageInfo imageInfo = new ImageInfo(tempFile);

            if (imageInfo.isAnimated()) {
                throw new RuntimeException("暂不支持动图");
            }

            // 5. 生成标签
            log.info("生成图片标签...");
            Map<String, Double> tagsMap;
            try {
                double threshold = systemSettingService.getDoubleSetting("tag.threshold");
                tagsMap = tagService.tagImage(objectName, threshold);
                log.info("生成了 {} 个标签", tagsMap.size());
            } catch (Exception e) {
                throw new RuntimeException("标签生成失败: " + e.getMessage(), e);
            }

            // 6. 生成图片 embedding
            log.info("生成图片 embedding...");
            double[] embedding;
            try {
                embedding = embeddingService.generateImageEmbedding(objectName);
                log.info("Embedding 生成完成，维度: {}", embedding.length);
            } catch (Exception e) {
                throw new RuntimeException("Embedding生成失败: " + e.getMessage(), e);
            }

            // 7. 创建并保存 Image 实体
            log.info("保存图片信息到数据库...");
            Image image = new Image();
            image.setTitle(title);
            image.setFileName(filename);
            image.setExtension(imageInfo.getExtension());
            image.setSize(size);
            image.setWidth(imageInfo.getWidth());
            image.setHeight(imageInfo.getHeight());
            image.setHash(hash);
            image.setEmbedding(embedding);

            // 设置标签关系（需要在 Image 创建后设置关联）
            for (Map.Entry<String, Double> entry : tagsMap.entrySet()) {
                Tag tag = tagService.getTagByName(entry.getKey());
                image.addTag(tag, entry.getValue());
            }

            imageService.addImage(image);

            // 8. 移动文件到正式目录
            log.info("移动文件到正式存储目录...");
            storageService.copyFile(objectName, "original/" + hash);
        } catch (Exception e) {
            throw new RuntimeException("处理图片失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            storageService.deleteFile(objectName);
            tempFile.delete();
        }
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