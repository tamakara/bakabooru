package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.system.entity.SystemSetting;
import com.tamakara.bakabooru.module.system.repository.SystemSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock private SystemSettingRepository repository;

    private SystemSettingService service;
    private List<SystemSetting> settings;

    @BeforeEach
    void setUp() {
        service = new SystemSettingService(repository);
        settings = List.of(
                new SystemSetting(SystemSettingService.TAG_THRESHOLD, "0.61"),
                new SystemSetting(SystemSettingService.AI_MAX_ATTEMPTS, "5"),
                new SystemSetting(SystemSettingService.AI_RETRY_BASE_DELAY_SECONDS, "30"),
                new SystemSetting(SystemSettingService.AI_RETRY_MAX_DELAY_SECONDS, "1800"),
                new SystemSetting(SystemSettingService.UPLOAD_COMPLETED_RETENTION_DAYS, "7")
        );
        lenient().when(repository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<String> requested = invocation.getArgument(0);
            Set<String> keys = StreamSupport.stream(requested.spliterator(), false)
                    .collect(java.util.stream.Collectors.toSet());
            return settings.stream().filter(setting -> keys.contains(setting.getKey())).toList();
        });
    }

    @Test
    void editableSettingsExcludeInternalValues() {
        Map<String, String> result = service.getEditableSettings();

        assertThat(result).containsOnlyKeys(
                SystemSettingService.TAG_THRESHOLD,
                SystemSettingService.AI_MAX_ATTEMPTS,
                SystemSettingService.AI_RETRY_BASE_DELAY_SECONDS,
                SystemSettingService.AI_RETRY_MAX_DELAY_SECONDS,
                SystemSettingService.UPLOAD_COMPLETED_RETENTION_DAYS
        );
        assertThat(result).doesNotContainKey("system.auth-password");
    }

    @Test
    void rejectsUnknownOrInternalKeys() {
        assertThatThrownBy(() -> service.updateEditableSettings(Map.of("system.auth-password", "secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-only");

        verify(repository, never()).saveAll(any());
    }

    @Test
    void validatesRangesAndNumberFormats() {
        assertThatThrownBy(() -> service.updateEditableSettings(Map.of(
                SystemSettingService.TAG_THRESHOLD, "not-a-number")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a number");

        assertThatThrownBy(() -> service.updateEditableSettings(Map.of(
                SystemSettingService.AI_MAX_ATTEMPTS, "21")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between");
    }

    @Test
    void validatesRetryDelayRelationshipAcrossPartialUpdates() {
        assertThatThrownBy(() -> service.updateEditableSettings(Map.of(
                SystemSettingService.AI_RETRY_BASE_DELAY_SECONDS, "2000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SystemSettingService.AI_RETRY_MAX_DELAY_SECONDS);
    }

    @Test
    void savesValidEditableSettings() {
        service.updateEditableSettings(Map.of(
                SystemSettingService.TAG_THRESHOLD, "0.75",
                SystemSettingService.AI_MAX_ATTEMPTS, "8"
        ));

        verify(repository).saveAll(any());
        assertThat(settings.stream()
                .filter(setting -> setting.getKey().equals(SystemSettingService.TAG_THRESHOLD))
                .findFirst().orElseThrow().getValue()).isEqualTo("0.75");
    }
}
