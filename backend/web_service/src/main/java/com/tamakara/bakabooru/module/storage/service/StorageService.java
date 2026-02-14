package com.tamakara.bakabooru.module.storage.service;

import com.tamakara.bakabooru.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public void uploadFile(String objectName, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(args);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    public void removeFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage());
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

    public InputStream getFileStream(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取文件流失败: " + objectName, e);
        }
    }
}
