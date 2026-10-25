package com.tamakara.bakabooru.module.setting.service;

import com.tamakara.bakabooru.module.setting.entity.Setting;
import com.tamakara.bakabooru.module.setting.repository.SettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SettingServiceTest {
    private SettingRepository repository;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private SettingService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        repository = mock(SettingRepository.class);
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new SettingService(repository, redis);
    }

    @Test
    void cacheHitSkipsDatabase() {
        when(values.get("bakabooru:setting:analysis.tag-threshold")).thenReturn("0.8");

        assertEquals("0.8", service.getValue(SettingService.TAG_THRESHOLD, "0.61"));
        verifyNoInteractions(repository);
    }

    @Test
    void cacheMissLoadsAndCachesDatabaseValue() {
        when(repository.findById(SettingService.TAG_THRESHOLD))
                .thenReturn(Optional.of(new Setting(SettingService.TAG_THRESHOLD, "0.72")));

        assertEquals("0.72", service.getValue(SettingService.TAG_THRESHOLD, "0.61"));
        verify(values).set("bakabooru:setting:analysis.tag-threshold", "0.72");
    }

    @Test
    void updateEvictsChangedKey() {
        Setting setting = new Setting(SettingService.TAG_THRESHOLD, "0.61");
        when(repository.findAllById(any())).thenReturn(List.of(setting));

        service.updateSettings(Map.of(SettingService.TAG_THRESHOLD, "0.9"));

        assertEquals("0.9", setting.getValue());
        verify(redis).delete("bakabooru:setting:analysis.tag-threshold");
    }
}
