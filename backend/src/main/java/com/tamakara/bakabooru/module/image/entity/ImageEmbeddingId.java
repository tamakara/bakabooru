package com.tamakara.bakabooru.module.image.entity;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ImageEmbeddingId implements Serializable {
    private Long image;
    private String modelId;
    private String modelRevision;
}
