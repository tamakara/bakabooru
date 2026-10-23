package com.tamakara.bakabooru.module.upload.dto;

import com.tamakara.bakabooru.module.upload.entity.UploadJob;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadTaskDto {
    private String id;
    private String filename;
    private long size;
    private String errorMessage;

    public static UploadTaskDto from(UploadJob job) {
        return new UploadTaskDto(
                job.getId().toString(),
                job.getFilename(),
                job.getSize(),
                job.getErrorMessage()
        );
    }
}
