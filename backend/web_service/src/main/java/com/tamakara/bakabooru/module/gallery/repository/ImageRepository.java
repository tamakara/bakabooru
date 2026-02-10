package com.tamakara.bakabooru.module.gallery.repository;

import com.tamakara.bakabooru.module.gallery.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long>, JpaSpecificationExecutor<Image> {
    Optional<Image> findByHash(String hash);
}

