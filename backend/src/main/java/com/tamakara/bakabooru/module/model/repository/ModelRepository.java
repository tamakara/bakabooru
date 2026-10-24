package com.tamakara.bakabooru.module.model.repository;

import com.tamakara.bakabooru.module.model.entity.Model;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelRepository extends JpaRepository<Model, String> {
    List<Model> findAllByOrderByNameAsc();
}
