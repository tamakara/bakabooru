package com.tamakara.bakabooru.module.file.service;

import com.tamakara.bakabooru.config.AppPaths;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final AppPaths appPaths;

    public void storePendingImage(String taskId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("文件为空.");
            }
            // 待处理文件路径
            Path pendingFile = appPaths.getPendingDir().resolve(taskId).normalize().toAbsolutePath();
            // 存储文件
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, pendingFile, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            throw new RuntimeException("待处理文件存储失败.", e);
        }
    }

    public void storeImage(String taskId, String hash) {
        try {
            Path pendingFile = appPaths.getPendingDir().resolve(taskId).normalize().toAbsolutePath();
            Path destinationFile = appPaths.getImageDir().resolve(hash).normalize().toAbsolutePath();
            Files.copy(pendingFile, destinationFile);
        } catch (Exception e) {
            throw new RuntimeException("图片存储失败.", e);
        }
    }

    public Path getImagePath(String hash) {
        return appPaths.getImageDir().resolve(hash);
    }

    public Path getPendingImagePath(String hash) {
        return appPaths.getPendingDir().resolve(hash);
    }

    public Path getThumbnailPath(String hash, int quality, int maxSize) {
        Path source = appPaths.getImageDir().resolve(hash);
        Path target = appPaths.getThumbnailDir().resolve(hash + "_" + maxSize + "_" + quality + ".jpg");

        if (Files.exists(target)) {
            return target;
        }

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(target)) {

            Thumbnails.of(in)
                    .size(maxSize, maxSize)
                    .outputQuality(quality / 100.0)
                    .outputFormat("jpg")
                    .toOutputStream(out);

        } catch (IOException e) {
            throw new RuntimeException("生成缩略图失败.", e);
        }

        return target;
    }

    public void clearCache() {
        Path dir = appPaths.getTempDir();
        if (!Files.exists(dir)) return;

        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("清除缓存失败.", e);
        }
    }
}
