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
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

        // 濠电姭鎷冮崨顓濈捕婵犳鍠氶崑銈咁嚕婵犳艾唯鐟滃海绮诲▎鎰闁糕€崇箰婢ф煡鏌?
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
                    relation.setSourceModelId(null);
                    relation.setScore(1.0);
                }, () -> image.addTag(tag, 1.0));
        return imageMapper.toDto(imageRepository.save(image));
    }

    @Transactional
    public ImageDto removeTag(Long id, Long tagId) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found: " + id));

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
            throw new IllegalArgumentException("At least one AI model is required");
        }
        aiJobService.enqueue(image, tagModelId, vectorModelIds);
        return imageMapper.toDto(image);
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
                throw new RuntimeException("闂備礁鎲＄敮鐐寸箾閳ь剚绻涢崨顓㈠弰鐎规洖鐖煎畷婊嗩槻妞わ綀顕ч…鍧楀箚閹殿喚缈遍柣?(ID: " + id + "): " + e.getMessage(), e);
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
                    // 濠电偠鎻紞鈧繛澶嬫礋瀵?ID_闂備礁鎼粔鏉懨洪顫偓?闂備礁婀遍。浠嬪疾濞戙垺鍎撶€广儱顦憴?闂備礁鎼粔鍫曞储瑜忓Σ鎰版晸閻樻枼鎸€闂佸憡鐟ラˇ浠嬪礈妤ｅ啯鐓涢柛灞剧閻绻涢崱鎰伈鐎规洏鍎遍濂稿川椤撶喐鐦ｇ紓?
                    String fileName = String.format("%d_%s.%s", image.getId(), image.getTitle(), image.getExtension());
                    zos.putNextEntry(new ZipEntry(fileName));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("闂備胶鎳撻悘姘跺箰閸濄儮鍋撻崹顐嗘垹绮欐径灞稿亾閿濆骸骞楃紒浣规緲椤潡骞嗛幍顔剧勘闁? " + e.getMessage(), e);
        }
    }
}
