package com.tamakara.bakabooru.module.image.dto;

import lombok.Data;

import java.util.List;

/**
 * 闁搞儱澧芥晶鏍磽閳哄啯娈ｉ柛銉хTO闁挎稑鐬奸弫銈嗙鎼淬垺鍋濈紒渚垮灩閸亞鎮伴妸銉ф綌缂佲偓?
 * 闁告瑯浜滅€垫﹢宕ラ銏㈢畱閻熸洑鑳跺▓鎴﹀春閻戞ɑ鎷卞ǎ鍥ｅ墲娴煎懘鏁嶇仦钘夋閻忓繑鍨堕弳鐔煎箲椤旇崵鐐婇弶鍫熸崌閸?
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

