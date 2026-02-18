package com.tamakara.bakabooru.module.tag.repository;

import com.tamakara.bakabooru.module.tag.dto.TagDto;
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

    List<Tag> findByNameContainingIgnoreCase(String name);


    @Query("SELECT new com.tamakara.bakabooru.module.tag.dto.TagDto(t.id, t.name, t.type) " +
            "FROM Tag t " +
            "WHERE t.name ILIKE CONCAT('%', :query, '%') " +
            "ORDER BY " +
            "  CASE WHEN t.name ILIKE CONCAT(:query, '%') THEN 0 ELSE 1 END, " +
            "  LENGTH(t.name) ASC")
    List<TagDto> searchTagsOptimized(@Param("query") String query, Pageable pageable);

}
