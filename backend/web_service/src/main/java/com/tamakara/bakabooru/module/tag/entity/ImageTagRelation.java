package com.tamakara.bakabooru.module.tag.entity;


import com.tamakara.bakabooru.module.image.entity.Image;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "image_tag_relation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"image_id", "tag_id"})
})
public class ImageTagRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "score", nullable = false)
    private Double score;

    public ImageTagRelation(Image image, Tag tag, Double score) {
        this.image = image;
        this.tag = tag;
        this.score = score;
    }
}