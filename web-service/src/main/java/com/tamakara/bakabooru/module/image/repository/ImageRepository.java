package com.tamakara.bakabooru.module.image.repository;

import com.tamakara.bakabooru.module.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {
    Optional<Image> findByHash(String hash);

    List<Image> findByStatus(String status);

    @Query("select coalesce(sum(i.size), 0) from Image i")
    long sumImageSize();
}


