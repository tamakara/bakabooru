package com.tamakara.bakabooru.module.gallery.service;

import com.tamakara.bakabooru.module.ai.service.AiProcessingService;
import com.tamakara.bakabooru.module.gallery.model.ImageInfo;
import com.tamakara.bakabooru.module.gallery.model.UploadTask;
import com.tamakara.bakabooru.module.gallery.model.UploadTaskQueue;
import com.tamakara.bakabooru.module.gallery.dto.TasksInfoDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.image.service.ThumbnailService;
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
    private final ImageService imageService;
    private final ThumbnailService thumbnailService;
    private final AiProcessingService aiProcessingService;

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
        File tempFile = new File(task.getTempFilePath());

        try {
            // 1. 计算文件哈希
            String hash = calculateHash(tempFile);
            String objectName = "original/" + hash;

            // 2. 查重
            if (imageService.existImageByHash(hash)) {
                throw new RuntimeException("图片已存在 (Hash: " + hash + ")");
            }

            // 3. 解析图片信息
            ImageInfo imageInfo = new ImageInfo(tempFile);
            if (imageInfo.isAnimated()) {
                throw new UnsupportedOperationException("暂不支持动图");
            }

            // 4. 归档原图与固定缩略图
            log.info("上传原图: {}", objectName);
            storageService.uploadFile(objectName, tempFile);
            thumbnailService.generateAndUploadThumbnail(tempFile, hash);

            // 5. 保存元数据并异步触发 AI 后处理
            Image image = saveImageMetadata(task, imageInfo, hash);
            aiProcessingService.requestProcessing(image.getId());

        } catch (Exception e) {
            throw new RuntimeException("处理图片失败: " + e.getMessage(), e);
        } finally {
            // 清理临时资源
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private String calculateHash(File file) {
         try (InputStream stream = new FileInputStream(file)) {
            return DigestUtils.sha256Hex(stream);
        } catch (Exception e) {
            throw new RuntimeException("计算哈希失败", e);
        }
    }

    private Image saveImageMetadata(UploadTask task, ImageInfo info, String hash) {
        Image image = new Image();
        image.setTitle(FilenameUtils.getBaseName(task.getFilename()));
        image.setFileName(task.getFilename());
        image.setExtension(info.getExtension());
        image.setSize(task.getSize());
        image.setWidth(info.getWidth());
        image.setHeight(info.getHeight());
        image.setHash(hash);
        image.setAiStatus(AiProcessingService.STATUS_PENDING);

        return imageService.addImage(image);
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
