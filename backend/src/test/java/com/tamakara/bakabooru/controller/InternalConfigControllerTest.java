package com.tamakara.bakabooru.controller;

import com.tamakara.bakabooru.config.InternalConfigProperties;
import com.tamakara.bakabooru.module.setting.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalConfigControllerTest {
    private SettingService settings;
    private InternalConfigProperties properties;
    private InternalConfigController controller;

    @BeforeEach
    void setUp() {
        settings = mock(SettingService.class);
        properties = new InternalConfigProperties();
        properties.setInternalToken("test-token");
        controller = new InternalConfigController(settings, properties);
    }

    @Test
    void batchReturnsUnifiedValuesObject() {
        when(settings.getValue(SettingService.INFERENCE_CONCURRENCY, "1")).thenReturn("2");
        when(settings.getValue(SettingService.ACTIVE_CLIP, "")).thenReturn("clip-vit-base-patch32");

        assertEquals(Map.of("values", Map.of(
                SettingService.INFERENCE_CONCURRENCY, "2",
                SettingService.ACTIVE_CLIP, "clip-vit-base-patch32")),
                controller.getMany("inference.concurrency,model.active-clip-id", "test-token"));
        verify(settings).getValue(SettingService.INFERENCE_CONCURRENCY, "1");
        verify(settings).getValue(SettingService.ACTIVE_CLIP, "");
    }

    @Test
    void singleKeyUsesSameResponseShape() {
        when(settings.getValue(SettingService.INFERENCE_CONCURRENCY, "1")).thenReturn("1");
        assertEquals(Map.of("values", Map.of(SettingService.INFERENCE_CONCURRENCY, "1")),
                controller.get(SettingService.INFERENCE_CONCURRENCY, "test-token"));
    }

    @Test
    void rejectsMissingTokenAndUnknownKeys() {
        assertThrows(ResponseStatusException.class,
                () -> controller.getMany("inference.concurrency", "wrong-token"));
        assertThrows(ResponseStatusException.class,
                () -> controller.getMany("unknown.setting", "test-token"));
    }
}
