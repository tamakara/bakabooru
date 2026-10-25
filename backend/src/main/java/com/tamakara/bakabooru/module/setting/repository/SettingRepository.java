package com.tamakara.bakabooru.module.setting.repository;

import com.tamakara.bakabooru.module.setting.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {
}

