package com.tamakara.bakabooru.module.image.entity;

import com.tamakara.bakabooru.config.VectorConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import org.hibernate.annotations.ColumnTransformer;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "image_embeddings")
@IdClass(ImageEmbeddingId.class)
public class ImageEmbedding {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Id
    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Id
    @Column(name = "model_revision", nullable = false)
    private String modelRevision;

    @Convert(converter = VectorConverter.class)
    @Column(columnDefinition = "vector", nullable = false)
    @ColumnTransformer(write = "?::vector")
    private double[] embedding;

    @Column(nullable = false)
    private String status;
    private String errorMessage;
    private Instant computedAt;
}
