package com.tamakara.bakabooru.module.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TasksInfoDto {
    private long pendingCount;
    private UploadTaskDto processingTask;
    private List<UploadTaskDto> failedTasks;
}
