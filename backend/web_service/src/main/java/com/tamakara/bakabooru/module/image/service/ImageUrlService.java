package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class ImageUrlService {

    private final StorageService storageService;
    private final SystemSettingService systemSettingService;

    @Named("toImageUrl")
    public String getImageUrl(Image image) {
        String objectName = "original/" + image.getHash();
        if (!storageService.existFile(objectName)) {
            throw new RuntimeException("文件不存在: " + objectName);
        }
        String finename = image.getId() + "_" + image.getTitle() + "." + image.getExtension();
        return storageService.getFileUrl(objectName, finename, 24);
    }

    @Named("toThumbnailUrl")
    public String getThumbnailUrl(Image image) {
        int size = systemSettingService.getIntSetting("file.thumbnail.size");
        String thumbnailObjectName = "thumbnail/" + size + "/" + image.getHash();

        if (!storageService.existFile(thumbnailObjectName)) {
            try {
                String imageObjectName = "original/" + image.getHash();
                File tempFile = storageService.getFile(imageObjectName);
                // 创建单独的缩略图输出文件，避免 Thumbnails 自动添加扩展名的问题
                File thumbnailFile = new File(tempFile.getParent(), image.getHash() + "_thumb.jpg");
                Thumbnails.of(tempFile)
                        .size(size, size)
                        .outputFormat("jpg")
                        .toFile(thumbnailFile);
                storageService.uploadFile(thumbnailObjectName, thumbnailFile);
                tempFile.delete();
                thumbnailFile.delete();
            } catch (Exception e) {
                throw new RuntimeException("生成缩略图失败: " + e.getMessage(), e);
            }
        }

        return storageService.getFileUrl(thumbnailObjectName, image.getHash(), 24);
    }
}
