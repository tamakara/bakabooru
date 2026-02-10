package com.tamakara.bakabooru.module.upload.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class UploadTaskStore {

    // 线程安全的任务存储
    private final ConcurrentHashMap<String, UploadTask> taskMap = new ConcurrentHashMap<>();

    // 按创建顺序的任务ID队列，最新任务在前
    private final ConcurrentLinkedDeque<String> taskOrder = new ConcurrentLinkedDeque<>();

    // 添加新任务
    public void addTask(UploadTask task) {
        taskMap.put(task.getId(), task);
        taskOrder.addFirst(task.getId()); // 最新任务在前
    }

    // 根据ID获取任务
    public UploadTask getTask(String id) {
        return taskMap.get(id);
    }

    // 删除任务
    public void removeTask(String id) {
        taskMap.remove(id);
        taskOrder.remove(id); // O(n)，但删除频率低可接受
    }

    // 获取按创建时间倒序的所有任务列表
    public List<UploadTask> getAllTasks() {
        List<UploadTask> result = new ArrayList<>();
        for (String id : taskOrder) {
            UploadTask task = taskMap.get(id);
            if (task != null) {
                result.add(task);
            }
        }
        return result;
    }

    // 清理已完成或失败的任务
    public void clearCompletedOrFailed() {
        for (String id : taskOrder) {
            UploadTask task = taskMap.get(id);
            if (task != null && (task.getStatus() == UploadTask.UploadStatus.COMPLETED
                    || task.getStatus() == UploadTask.UploadStatus.FAILED)) {
                removeTask(id);
            }
        }
    }

    // 返回当前任务总数
    public int size() {
        return taskMap.size();
    }
}