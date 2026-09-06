package com.tamakara.bakabooru.module.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_models")
public class AiModel {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String capability;
    @Column(nullable = false)
    private String version;
    private Integer dimension;
    @Column(nullable = false)
    private String status;
    private String artifactObject;
    private String downloadUrl;
    private String artifactSha256;
    @Column(columnDefinition = "TEXT")
    private String preprocessingJson;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
}
