package com.tamakara.bakabooru.module.image.entity;

import com.tamakara.bakabooru.config.VectorConverter;
import com.tamakara.bakabooru.module.image.dto.ImageTagDto;
import com.tamakara.bakabooru.module.tag.entity.ImageTagRelation;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "images")
@EntityListeners(AuditingEntityListener.class)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String extension;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(unique = true, nullable = false)
    private String hash;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Convert(converter = VectorConverter.class)
    @Column(columnDefinition = "vector(512)")
    @ColumnTransformer(write = "?::vector")
    private double[] embedding;

    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ImageTagRelation> tagRelations = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public List<ImageTagDto> getTags() {
        List<ImageTagDto> tags = new ArrayList<>();
        for (ImageTagRelation relation : tagRelations) {
            Tag tag = relation.getTag();
            tags.add(new ImageTagDto(tag.getId(), tag.getName(), tag.getType(), relation.getScore()));
        }
        tags.sort(Comparator.comparing(ImageTagDto::getScore));
        return tags;
    }

    public void addTag(Tag tag, Double score) {
        ImageTagRelation relation = new ImageTagRelation(this, tag, score);
        this.tagRelations.add(relation);
    }

    public void deleteTag(Tag tag) {
        for (Iterator<ImageTagRelation> iterator = tagRelations.iterator(); iterator.hasNext(); ) {
            ImageTagRelation relation = iterator.next();
            if (relation.getImage().equals(this) && relation.getTag().equals(tag)) {
                iterator.remove();
                relation.setImage(null);
                relation.setTag(null);
            }
        }
    }
}
