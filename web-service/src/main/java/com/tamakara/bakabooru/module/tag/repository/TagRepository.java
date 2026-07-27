package com.tamakara.bakabooru.module.tag.repository;

import com.tamakara.bakabooru.module.tag.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("SELECT t FROM Tag t " +
            "WHERE LOWER(t.name) LIKE LOWER(CONCAT(:query, '%')) " +
            "ORDER BY t.name ASC")
    List<Tag> searchTags(@Param("query") String query, Pageable pageable);
}
