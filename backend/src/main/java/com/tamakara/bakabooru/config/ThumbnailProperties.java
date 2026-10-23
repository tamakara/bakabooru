package com.tamakara.bakabooru.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.thumbnail")
public class ThumbnailProperties {
    private int maxSize = 1024;
    private float quality = 0.85f;
    private String format = "jpg";
}
