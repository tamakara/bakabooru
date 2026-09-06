package com.tamakara.bakabooru.module.image.mapper;

import com.tamakara.bakabooru.module.image.dto.ImageDto;
import com.tamakara.bakabooru.module.image.dto.ImageThumbnailDto;
import com.tamakara.bakabooru.module.image.entity.Image;
import com.tamakara.bakabooru.module.image.service.ImageUrlService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;
import java.util.List;
import com.tamakara.bakabooru.module.image.dto.ImageVectorDto;
import com.tamakara.bakabooru.module.image.entity.ImageEmbedding;

@Mapper(componentModel = "spring", uses = {ImageUrlService.class})
public interface ImageMapper {
    @Mapping(target = "imageUrl", source = "image", qualifiedByName = "toImageUrl")
    @Mapping(target = "thumbnailUrl", source = "image", qualifiedByName = "toThumbnailUrl")
    @Mapping(target = "indexVectors", source = "indexVectors")
    ImageDto toDto(Image image);

    @Mapping(target = "thumbnailUrl", source = "image", qualifiedByName = "toThumbnailUrl")
    @Mapping(target = "imageUrl", source = "image", qualifiedByName = "toImageUrl")
    @Mapping(target = "indexVectorModelIds", source = "indexVectors")
    ImageThumbnailDto toThumbnailDto(Image image);

    default List<String> mapModelIds(Collection<com.tamakara.bakabooru.module.image.entity.ImageEmbedding> vectors) {
        if (vectors == null) return List.of();
        return vectors.stream().map(v -> v.getModelId()).distinct().toList();
    }

    default List<ImageVectorDto> mapVectors(Collection<ImageEmbedding> vectors) {
        if (vectors == null) return List.of();
        return vectors.stream().map(vector -> {
            ImageVectorDto dto = new ImageVectorDto();
            dto.setModelId(vector.getModelId());
            dto.setModelRevision(vector.getModelRevision());
            dto.setStatus(vector.getStatus());
            dto.setErrorMessage(vector.getErrorMessage());
            dto.setComputedAt(vector.getComputedAt());
            return dto;
        }).toList();
    }
}
