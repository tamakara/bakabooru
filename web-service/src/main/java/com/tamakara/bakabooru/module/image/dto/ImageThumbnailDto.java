package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.util.List;

/**
 * 鍥剧墖缂╃暐鍥綝TO锛岀敤浜庢悳绱㈠垪琛ㄥ睍绀?
 * 鍙寘鍚繀瑕佺殑鍩烘湰淇℃伅锛屽噺灏戞暟鎹紶杈撻噺
 */
@Data
public class ImageThumbnailDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String imageUrl;
    private String status;
    private List<String> indexVectorModelIds;
    private String tagModelId;
}

