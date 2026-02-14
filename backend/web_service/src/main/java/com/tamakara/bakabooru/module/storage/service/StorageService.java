package com.tamakara.bakabooru.module.storage.service;

import com.tamakara.bakabooru.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public void uploadFile(String objectName, File file) {
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .filename(file.getAbsolutePath())
                            .contentType(Files.probeContentType(file.toPath()))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败" + e.getMessage(), e);
        }
    }


    public void copyFile(String sourceObject, String targetObject) {
        try {
            String bucket = minioConfig.getBucketName();

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetObject)
                            .source(CopySource.builder().bucket(bucket).object(sourceObject).build())
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("文件复制失败 [" + sourceObject + " -> " + targetObject + "]: " + e.getMessage(), e);
        }
    }

    public void removeFile(String objectName) {
        try {
            String bucket = minioConfig.getBucketName();

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败 [ " + objectName + "]: " + e.getMessage());
        }
    }

    public String getFileUrl(String objectName, String originalFilename, int expiresHours) {
        try {
            String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            Map<String, String> extraParams = new HashMap<>();

            extraParams.put("response-content-disposition",
                    "inline; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);

            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(expiresHours, TimeUnit.HOURS)
                    .extraQueryParams(extraParams)
                    .build();
            String internalUrl = minioClient.getPresignedObjectUrl(args);
            String externalUrl = internalUrl.replace(minioConfig.getEndpoint(), minioConfig.getPublicEndpoint());
            return externalUrl;
        } catch (Exception e) {
            throw new RuntimeException("获取文件URL失败: " + e.getMessage());
        }
    }


}
