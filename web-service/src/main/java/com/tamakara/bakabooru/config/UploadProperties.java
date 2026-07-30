package com.tamakara.bakabooru.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private Duration lockDuration = Duration.ofMinutes(2);
}
