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
import java.nio.file.StandardCopyOption;
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
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = detectContentType(file.getName());
            }
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .filename(file.getAbsolutePath())
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败" + e.getMessage(), e);
        }
    }

    /**
     * 根据文件名扩展名推断 Content-Type
     */
    private String detectContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerName.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lowerName.endsWith(".tiff") || lowerName.endsWith(".tif")) {
            return "image/tiff";
        } else if (lowerName.endsWith(".avif")) {
            return "image/avif";
        }
        return "application/octet-stream";
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

    public void deleteFile(String objectName) {
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

    public Boolean existFile(String objectName) {
        try {
            String bucket = minioConfig.getBucketName();
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("检查文件存在失败 [ " + objectName + "]: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("检查文件存在失败 [ " + objectName + "]: " + e.getMessage());
        }
    }

    public File getFile(String objectName) {
        try {
            String bucket = minioConfig.getBucketName();
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            File tempFile = File.createTempFile("minio-", ".tmp");
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("获取文件失败 [ " + objectName + "]: " + e.getMessage());
        }
    }

    public String getFileUrl(String objectName, String filename, int expiresHours) {
        try {
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
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
