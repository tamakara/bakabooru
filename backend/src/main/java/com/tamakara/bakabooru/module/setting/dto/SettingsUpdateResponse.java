package com.tamakara.bakabooru.module.setting.dto;

import java.util.List;

public record SettingsUpdateResponse(
        boolean updated,
        boolean restartRequired,
        List<String> restartServices,
        String message
) {}
