package com.tamakara.bakabooru.module.image.service;

import com.tamakara.bakabooru.config.ThumbnailProperties;
import com.tamakara.bakabooru.module.image.entity.Image;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageUrlService {

    private final StorageService storageService;
    private final ThumbnailProperties thumbnailProperties;

    @Named("toImageUrl")
    public String getImageUrl(Image image) {
        return getImageUrl(image.getHash(), image.getId(), image.getTitle(), image.getExtension());
    }

    @Named("toThumbnailUrl")
    public String getThumbnailUrl(Image image) {
        return getThumbnailUrl(image.getHash());
    }

    @Named("resolveThumbnailUrl")
    public String getThumbnailUrl(String hash) {
        String thumbnailObjectName = getThumbnailObjectName(hash);
        return storageService.getFileUrl(thumbnailObjectName, hash + "." + thumbnailProperties.getFormat(), 24);
    }

    @Named("resolveImageUrl")
    public String getImageUrl(String hash, Long id, String title, String extension) {
        String objectName = "original/" + hash;
        String filename = id + "_" + title + "." + extension;
        return storageService.getFileUrl(objectName, filename, 24);
    }

    @Named("resolveThumbnailObjectName")
    public String getThumbnailObjectName(String hash) {
        return "thumbnail/" + thumbnailProperties.getMaxSize() + "/" + hash + "." + thumbnailProperties.getFormat();
    }
}
