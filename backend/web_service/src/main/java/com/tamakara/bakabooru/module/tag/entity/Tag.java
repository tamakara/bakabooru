package com.tamakara.bakabooru.module.tag.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @OneToMany(mappedBy = "tag")
    private Set<ImageTagRelation> imageRelations = new HashSet<>();

    public Tag(String name, String type) {
        this.name = name;
        this.type = type;
    }
}

