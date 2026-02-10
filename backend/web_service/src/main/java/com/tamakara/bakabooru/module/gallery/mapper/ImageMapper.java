package com.tamakara.bakabooru.module.gallery.mapper;

import com.tamakara.bakabooru.module.file.service.SignatureService;
import com.tamakara.bakabooru.module.gallery.dto.ImageDto;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.tag.mapper.TagMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface ImageMapper {
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    ImageDto toDto(Image image, @Context SignatureService signatureService);

    @AfterMapping
    default void setUrl(@MappingTarget ImageDto dto, Image image, @Context SignatureService signatureService) {
        if (image.getHash() != null) {
            // 生成签名 URL
            //TODO: 自定义过期时间
            String signedUrl = signatureService.generateSignedUrl("/api/file/" + image.getHash(), 1000 * 60 * 60);
            String signedThumbnailUrl = signatureService.generateSignedUrl("/api/file/thumb/" + image.getHash(), 1000 * 60 * 60);

            dto.setUrl(signedUrl);
            dto.setThumbnailUrl(signedThumbnailUrl);
        }
    }
}

