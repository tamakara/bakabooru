package com.tamakara.bakabooru.module.image.mapper;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageService;
import com.tamakara.bakabooru.module.tag.mapper.TagMapper;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ImageService.class})
public interface ImageMapper {
    @Mapping(target = "imageUrl", source = "image", qualifiedByName = "toImageUrl")
    @Mapping(target = "thumbnailUrl", source = "image", qualifiedByName = "toThumbnailUrl")
    ImageDto toDto(Image image);
}