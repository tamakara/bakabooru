package com.tamakara.bakabooru.module.gallery.dto;

import com.tamakara.bakabooru.module.tag.dto.TagDto;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ImageDto {
    private Long id;
    private String title;
    private String fileName;
    private String extension;
    private Long size;
    private Integer width;
    private Integer height;
    private String hash;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TagDto> tags;
    private String url;
    private String thumbnailUrl;
}
