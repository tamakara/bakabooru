package com.tamakara.bakabooru.module.upload.dto;

import com.tamakara.bakabooru.module.upload.model.UploadTask;
import lombok.Data;

import java.util.List;

@Data
public class TasksInfoDto {
    private Long pendingCount;
    private UploadTask processingTask;
    private List<UploadTask> failedTasks;
}
