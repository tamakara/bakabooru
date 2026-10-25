package com.tamakara.bakabooru.module.system.dto;

import java.util.List;

public record SettingsUpdateResponse(
        boolean updated,
        boolean restartRequired,
        List<String> restartServices,
        String message
) {}
