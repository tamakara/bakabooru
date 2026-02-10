package com.tamakara.bakabooru.module.gallery.entity;

import com.tamakara.bakabooru.module.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "images")
@Getter
@Setter
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

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> tags = new ArrayList<>();

    // ==========================================
    // 4. 高级：向量数据 (pgvector)
    // ==========================================
    /**
     * 注意：直接映射 vector 类型比较复杂，通常建议：
     * A. 只要存取，不涉及复杂计算：映射为 List<Float>
     * B. 需要复杂计算：通常由 Python 端或原生 SQL 处理
     * 这里演示最简单的存储映射。
     */
    @Column(columnDefinition = "vector(384)") // 假设你的模型维度是 384
    @JdbcTypeCode(SqlTypes.ARRAY)             // 作为一个数组处理
    private List<Float> embedding;
}
