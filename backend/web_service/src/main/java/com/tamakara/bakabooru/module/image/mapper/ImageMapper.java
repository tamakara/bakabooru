package com.tamakara.bakabooru.module.image.mapper;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageUrlService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ImageUrlService.class})
public interface ImageMapper {
    @Mapping(target = "imageUrl", source = "image", qualifiedByName = "toImageUrl")
    @Mapping(target = "thumbnailUrl", source = "image", qualifiedByName = "toThumbnailUrl")
    ImageDto toDto(Image image);
}