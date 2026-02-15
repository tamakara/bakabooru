package com.tamakara.bakabooru.module.gallery.model;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UploadTaskQueue {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1. 待处理队列 (List)
    private static final String KEY_QUEUE = "upload:task:queue";
    // 2. 失败队列 (List)
    private static final String KEY_FAILED_QUEUE = "upload:task:failed";
    // 3. 任务数据 (String KV)
    private static final String KEY_DATA_PREFIX = "upload:task:data:";

    /**
     * 添加新任务 (进入等待队列)
     */
    public void addTask(UploadTask task) {
        // 存数据 (建议过期时间设长一点，防止失败后还在排队但数据没了，比如 3 天)
        redisTemplate.opsForValue().set(KEY_DATA_PREFIX + task.getId(), task, 3, TimeUnit.DAYS);
        // 入队
        redisTemplate.opsForList().leftPush(KEY_QUEUE, task.getId());
    }

    /**
     * 阻塞获取任务
     */
    public String takeTask(int timeoutSeconds) {
        Object obj = redisTemplate.opsForList().rightPop(KEY_QUEUE, timeoutSeconds, TimeUnit.SECONDS);
        return obj != null ? obj.toString() : null;
    }

    public UploadTask getTask(String id) {
        return (UploadTask) redisTemplate.opsForValue().get(KEY_DATA_PREFIX + id);
    }

    /**
     * 【新增】将任务移动到失败队列
     * 当消费者处理抛出异常时调用此方法
     */
    public void moveToFailed(String id, String errorMessage) {
        // 1. 更新任务状态和错误信息
        UploadTask task = getTask(id);
        if (task != null) {
            task.setErrorMessage(errorMessage);
            // 更新 Redis 里的数据
            redisTemplate.opsForValue().set(KEY_DATA_PREFIX + id, task, 3, TimeUnit.DAYS);
        }

        // 2. 将 ID 推入失败队列
        redisTemplate.opsForList().leftPush(KEY_FAILED_QUEUE, id);
    }

    /**
     * 重试任务 (从失败队列 -> 等待队列)
     */
    public void retryTask(String id) {
        // 1. 从失败队列移除该 ID (count=1, value=id)
        Long count = redisTemplate.opsForList().remove(KEY_FAILED_QUEUE, 1, id);

        if (count != null && count > 0) {
            // 2. 更新状态为 PENDING
            UploadTask task = getTask(id);
            if (task != null) {
                task.setErrorMessage(null); // 清空错误信息
                redisTemplate.opsForValue().set(KEY_DATA_PREFIX + id, task, 3, TimeUnit.DAYS);

                // 3. 重新入队 (进入等待队列头部或尾部均可，这里放头部优先处理)
                redisTemplate.opsForList().rightPush(KEY_QUEUE, id);
            }
        }
    }

    public void clearFailedTasks() {
        List<UploadTask> failedTasks = getFailedTasks();
        if (failedTasks.isEmpty()) return;
        for(UploadTask task : failedTasks) {
            new File(task.getTempFilePath()).delete();
            redisTemplate.delete(KEY_DATA_PREFIX + task.getId());
        }
        redisTemplate.delete(KEY_FAILED_QUEUE);
    }

    /**
     * 获取所有失败的任务列表
     */
    public List<UploadTask> getFailedTasks() {
        List<Object> ids = redisTemplate.opsForList().range(KEY_FAILED_QUEUE, 0, -1);
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        List<String> keys = ids.stream()
                .map(id -> KEY_DATA_PREFIX + id)
                .collect(Collectors.toList());

        List<Object> results = redisTemplate.opsForValue().multiGet(keys);

        if (results == null) return new ArrayList<>();
        return results.stream()
                .filter(Objects::nonNull)
                .map(obj -> (UploadTask) obj)
                .collect(Collectors.toList());
    }

    public void removeTaskData(String id) {
        redisTemplate.delete(KEY_DATA_PREFIX + id);
    }

    public long getPendingCount() {
        Long size = redisTemplate.opsForList().size(KEY_QUEUE);
        return size != null ? size : 0;
    }
}