package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.ImageTagDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.storage.service.StorageService;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import com.tamakara.bakabooru.module.tag.dto.TagDto;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
import jdk.jfr.Name;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final StorageService storageService;
    private final TagService tagService;
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
                Thumbnails.of(tempFile)
                        .size(size, size)
                        .outputFormat("jpg")
                        .toFile(tempFile);
                storageService.uploadFile(thumbnailObjectName, tempFile);
                tempFile.delete();
            } catch (Exception e) {
                throw new RuntimeException("生成缩略图失败: " + e.getMessage(), e);
            }
        }

        return storageService.getFileUrl(thumbnailObjectName, image.getHash(), 24);
    }

    @Transactional
    public void addImage(Image image) {
        imageRepository.save(image);
    }

    public boolean existImageByHash(String hash) {
        return imageRepository.findByHash(hash).isPresent();
    }

    @Transactional
    public ImageDto updateImage(Long id, ImageDto dto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(Instant.now());
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto addTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));
        Tag tag = tagService.getTagById(tagId);
        image.addTag(tag, 1.0);
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        image.getTags().removeIf(tag -> tag.getId().equals(tagId));
        image.setUpdatedAt(Instant.now());

        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到图片"));

        String objectName = "original/" + image.getHash();
        storageService.deleteFile(objectName);

        imageRepository.delete(image);
    }


    @Transactional
    public void deleteImages(List<Long> ids) {
        ids.forEach(id -> {
            try {
                deleteImage(id);
            } catch (Exception e) {
                throw new RuntimeException("删除图片失败 (ID: " + id + "): " + e.getMessage(), e);
            }
        });
    }

    @Transactional(readOnly = true)
    public void downloadImages(List<Long> ids, OutputStream outputStream) {
        List<Image> images = imageRepository.findAllById(ids);
        if (images.isEmpty()) return;

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Image image : images) {
                String objectName = "original/" + image.getHash();
                File file = storageService.getFile(objectName);
                if (file.exists()) {
                    String entryName = image.getId() + "_" + image.getTitle() + "." + image.getExtension();
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("打包下载失败: " + e.getMessage(), e);
        }
    }
}
