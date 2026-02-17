package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

/**
 * 图片缩略图DTO，用于搜索列表展示
 * 只包含必要的基本信息，减少数据传输量
 */
@Data
public class ImageThumbnailDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
}
