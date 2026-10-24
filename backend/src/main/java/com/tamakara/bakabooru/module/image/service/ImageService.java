package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.image.entity.Tag;
import com.tamakara.bakabooru.module.image.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final StorageService storageService;
    private final TagService tagService;

    @Transactional
    public ImageDto getImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        // 查看详情时记录访问次数。
        image.setViewCount(image.getViewCount() + 1);
        imageRepository.save(image);

        return imageMapper.toDto(image);
    }

    @Transactional
    public Image addImage(Image image) {
        return imageRepository.save(image);
    }

    public boolean existImageByHash(String hash) {
        return imageRepository.findByHash(hash).isPresent();
    }

    @Transactional
    public ImageDto updateImage(Long id, ImageDto dto) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(Instant.now());
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto addTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        Tag tag = tagService.getTagById(tagId);
        image.getTagRelations().stream()
                .filter(relation -> relation.getTag().getId().equals(tagId))
                .findFirst()
                .ifPresentOrElse(relation -> {
                    relation.setSourceType("MANUAL");
                    relation.setScore(1.0);
                }, () -> image.addTag(tag, 1.0));
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        Tag tag = tagService.getTagById(tagId);
        image.deleteTag(tag);
        image.setUpdatedAt(Instant.now());

        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

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

    @Transactional
    public int deleteMissingImages() {
        List<Image> missing = imageRepository.findByStatus("MISSING");
        for (Image image : missing) {
            try {
                storageService.deleteFile("original/" + image.getHash());
                storageService.deleteFile("thumbnail/" + image.getHash());
            } catch (Exception ignored) {
                // Missing objects are ignored; the database record is still removed.
            }
        }
        return missing.size();
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
                    // 使用图片 ID 前缀避免归档中同名文件冲突。
                    String fileName = String.format("%d_%s.%s", image.getId(), image.getTitle(), image.getExtension());
                    zos.putNextEntry(new ZipEntry(fileName));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("打包下载图片失败: " + e.getMessage(), e);
        }
    }
}
