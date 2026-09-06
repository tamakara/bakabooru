package com.tamakara.bakabooru.module.image.repository;

import com.tamakara.bakabooru.module.image.entity.ImageEmbedding;
import com.tamakara.bakabooru.module.image.entity.ImageEmbeddingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ImageEmbeddingRepository extends JpaRepository<ImageEmbedding, ImageEmbeddingId> {
    @Query("select e from ImageEmbedding e join fetch e.image where e.image.id = :imageId order by e.modelId")
    List<ImageEmbedding> findForImage(Long imageId);
}
