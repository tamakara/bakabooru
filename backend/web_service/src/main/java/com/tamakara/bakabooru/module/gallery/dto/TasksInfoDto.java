package com.tamakara.bakabooru.module.gallery.dto;

import com.tamakara.bakabooru.module.gallery.model.UploadTask;
import lombok.Data;

import java.util.List;

@Data
public class TasksInfoDto {
    private Long pendingCount;
    private UploadTask processingTask;
    private List<UploadTask> failedTasks;
}
