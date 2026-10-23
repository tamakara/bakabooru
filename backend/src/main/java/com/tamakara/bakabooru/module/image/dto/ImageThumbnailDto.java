package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.util.List;

/** 图库列表使用的轻量图片信息。 */
@Data
public class ImageThumbnailDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String imageUrl;
    private String status;
    private List<String> indexVectorModelIds;
}
