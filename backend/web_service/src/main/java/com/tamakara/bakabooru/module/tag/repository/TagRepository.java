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

    List<Tag> findByNameContainingIgnoreCase(String name);

    /**
     * 优化的标签搜索查询，使用数据库级别的排序和分页
     * 优先返回以查询字符串开头的标签，然后按名称长度排序
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY CASE WHEN LOWER(t.name) LIKE LOWER(CONCAT(:query, '%')) THEN 0 ELSE 1 END, " +
           "LENGTH(t.name) ASC")
    List<Tag> searchTagsOptimized(@Param("query") String query, Pageable pageable);
}
