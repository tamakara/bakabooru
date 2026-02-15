package com.tamakara.bakabooru.module.image.mapper;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.tag.mapper.TagMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface ImageMapper {
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    ImageDto toDto(Image image);

    @AfterMapping
    default void setUrl(@MappingTarget ImageDto dto, Image image) {
        if (image.getHash() != null) {
            dto.setUrl();
            dto.setThumbnailUrl();
        }
    }
}

