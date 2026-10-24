package com.tamakara.bakabooru.module.image.mapper;

import com.tamakara.bakabooru.module.image.dto.TagDto;
import com.tamakara.bakabooru.module.image.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDto(Tag tag);
}

