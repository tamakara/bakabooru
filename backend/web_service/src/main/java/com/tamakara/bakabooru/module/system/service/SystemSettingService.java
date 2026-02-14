package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY = "system:settings";

    /**
     * 系统启动时，将数据库所有配置加载到 Redis (Cache Warm-up)
     */
    @PostConstruct
    public void initCache() {
        log.info("正在初始化系统设置缓存...");
        refreshCache();
    }

    /**
     * 强制刷新缓存：DB -> Redis
     */
    public void refreshCache() {
        List<SystemSetting> all = systemSettingRepository.findAll();

        // 转换 List 为 Map<String, String>
        Map<String, String> map = all.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));

        // 删除旧缓存并重新写入
        stringRedisTemplate.delete(REDIS_KEY);
        if (!map.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(REDIS_KEY, map);
        }
    }

    /**
     * 获取所有配置
     * 优化：将 Redis 的 Map<Object, Object> 转换为 Map<String, String>
     */
    public Map<String, String> getAllSettings() {
        // 1. 从 Redis 获取所有键值对
        Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(REDIS_KEY);

        // 2. 如果 Redis 为空（可能是被清理了），尝试回源数据库加载
        if (rawMap.isEmpty()) {
            refreshCache();
            rawMap = stringRedisTemplate.opsForHash().entries(REDIS_KEY);
        }

        // 3. 类型安全转换 (Object -> String)
        Map<String, String> resultMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            resultMap.put((String) entry.getKey(), (String) entry.getValue());
        }

        return resultMap;
    }

    public String getSetting(String key) {
        // opsForHash().get() 返回的是 Object，需要强转
        Object val = stringRedisTemplate.opsForHash().get(REDIS_KEY, key);

        if (val == null) {
            // 缓存击穿保护：查库
            return systemSettingRepository.findById(key)
                    .map(setting -> {
                        // 查到后补回缓存
                        stringRedisTemplate.opsForHash().put(REDIS_KEY, key, setting.getValue());
                        return setting.getValue();
                    })
                    .orElseThrow(() -> new RuntimeException("Setting with key: " + key + " not found"));
        }
        return (String) val;
    }

    // --- 类型转换辅助方法 ---

    public Boolean getBooleanSetting(String key) {
        return "true".equalsIgnoreCase(getSetting(key));
    }

    public int getIntSetting(String key) {
        return Integer.parseInt(getSetting(key));
    }

    public long getLongSetting(String key) {
        return Long.parseLong(getSetting(key));
    }

    public double getDoubleSetting(String key) {
        return Double.parseDouble(getSetting(key));
    }

    /**
     * 单条更新
     */
    @Transactional
    public void updateSetting(String key, String value) {
        updateSettings(Collections.singletonMap(key, value));
    }

    /**
     * 批量更新 (深度优化版)
     */
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) {
            return;
        }

        Set<String> keys = newSettings.keySet();

        // 1. 批量查询 DB (1次 SQL: SELECT * FROM table WHERE id IN (...))
        List<SystemSetting> existingSettings = systemSettingRepository.findAllById(keys);

        // 2. 严格检查：确保所有 Key 都存在于数据库中
        if (existingSettings.size() != keys.size()) {
            // 找出哪个 Key 不存在 (可选，用于报错提示)
            Set<String> existingKeys = existingSettings.stream()
                    .map(SystemSetting::getKey)
                    .collect(Collectors.toSet());
            keys.removeAll(existingKeys);
            throw new RuntimeException("Update failed. The following keys do not exist: " + keys);
        }

        // 3. 在内存中更新 Entity 对象
        for (SystemSetting setting : existingSettings) {
            String newValue = newSettings.get(setting.getKey());
            setting.setValue(newValue);
        }

        // 4. 批量保存到 DB (1次 SQL 交互，取决于 JPA 实现，通常是 batch update)
        systemSettingRepository.saveAll(existingSettings);

        // 5. 批量更新 Redis (1次 Redis 网络交互: HMSET)
        stringRedisTemplate.opsForHash().putAll(REDIS_KEY, newSettings);
    }
}