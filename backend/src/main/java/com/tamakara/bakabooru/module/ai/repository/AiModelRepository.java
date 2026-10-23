package com.tamakara.bakabooru.module.ai.repository;

import com.tamakara.bakabooru.module.ai.entity.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiModelRepository extends JpaRepository<AiModel, String> {
    List<AiModel> findAllByOrderByNameAsc();
}
