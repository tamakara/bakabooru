package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.module.ai.service.AiJobService;
import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.mapper.ImageMapper;
import com.tamakara.bakabooru.module.image.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.service.TagService;
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
    private final AiJobService aiJobService;

    @Transactional
    public ImageDto getImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("闁归潧褰炵粭澶愬礆閺夋寧绂堥柣?)");

        // 濠⒀呭仜婵偤寮婚妷褎绠欐繛鍡忓墲閺?
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
                .orElseThrow(() -> new RuntimeException("闁归潧褰炵粭澶愬礆閺夋寧绂堥柣?)");

        if (dto.getTitle() != null) {
            image.setTitle(dto.getTitle());
        }
        image.setUpdatedAt(Instant.now());
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto addTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("闁归潧褰炵粭澶愬礆閺夋寧绂堥柣?)");
        Tag tag = tagService.getTagById(tagId);
        image.getTagRelations().stream()
                .filter(relation -> relation.getTag().getId().equals(tagId))
                .findFirst()
                .ifPresentOrElse(relation -> {
                    relation.setSourceType("MANUAL");
                    relation.setSourceModelId(null);
                    relation.setScore(1.0);
                }, () -> image.addTag(tag, 1.0));
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("闁归潧褰炵粭澶愬礆閺夋寧绂堥柣?)");

        image.getTags().removeIf(tag -> tag.getId().equals(tagId));
        image.setUpdatedAt(Instant.now());

        return imageMapper.toDto(imageRepository.save(image));
    }

    public ImageDto retryAiProcessing(Long id) {
        Image image = aiJobService.retry(id);
        return imageMapper.toDto(image);
    }

    @Transactional
    public ImageDto enqueueAi(Long id, String tagModelId, String vectorModelIds) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));
        if ((tagModelId == null || tagModelId.isBlank())
                && (vectorModelIds == null || vectorModelIds.isBlank())) {
            throw new IllegalArgumentException("闁煎嘲鍟块惃顖炴焻婢跺顏ュ☉鎾亾濞?AI 婵☆垪鈧磭鈧?");
        }
        aiJobService.enqueue(image, tagModelId, vectorModelIds);
        return imageMapper.toDto(image);
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("闁归潧褰炵粭澶愬礆閺夋寧绂堥柣?)");

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
                throw new RuntimeException("闁告帞濞€濞呭酣宕堕崜褍顣诲鎯扮簿鐟?(ID: " + id + "): " + e.getMessage(), e);
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
                // 闁告鍠庡ù姗€寮甸鈧銊╁矗椤栨繂鍘村☉鎾崇Т閻°劑宕烽…鎺斿耿闁轰胶澧楀畵浣规償閹捐鍞剁憸鐗堟磻缁稒鎯旈弬鍨仼闂?            }
            imageRepository.delete(image);
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
                    // 濞达綀娉曢弫?ID_闁哄秴娲。?闁圭鏅涢惈宥夊触?闁哄秶鍘х槐锟犳⒓閸欏鍓鹃柡鍌氭矗濞嗐垽宕ュ鍛毐缂?
                    String fileName = String.format("%d_%s.%s", image.getId(), image.getTitle(), image.getExtension());
                    zos.putNextEntry(new ZipEntry(fileName));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("闁瑰灚鎸哥€垫ɑ绋夌€ｎ厽绁板鎯扮簿鐟? " + e.getMessage(), e);
        }
    }
}
