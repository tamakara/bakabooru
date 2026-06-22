package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.config.ThumbnailProperties;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final StorageService storageService;
    private final ImageUrlService imageUrlService;
    private final ThumbnailProperties thumbnailProperties;

    public void generateAndUploadThumbnail(File sourceFile, String hash) {
        File thumbnailFile = null;
        try {
            thumbnailFile = File.createTempFile(hash + "-thumb-", "." + thumbnailProperties.getFormat());
            Thumbnails.of(sourceFile)
                    .size(thumbnailProperties.getMaxSize(), thumbnailProperties.getMaxSize())
                    .outputFormat(thumbnailProperties.getFormat())
                    .outputQuality(thumbnailProperties.getQuality())
                    .toFile(thumbnailFile);

            storageService.uploadFile(imageUrlService.getThumbnailObjectName(hash), thumbnailFile);
        } catch (Exception e) {
            throw new RuntimeException("生成缩略图失败: " + e.getMessage(), e);
        } finally {
            if (thumbnailFile != null && thumbnailFile.exists()) {
                thumbnailFile.delete();
            }
        }
    }
}
