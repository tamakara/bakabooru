package com.tamakara.bakabooru.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-job")
public class AiJobProperties {
    private Duration lockDuration = Duration.ofMinutes(5);
    private Duration retryBaseDelay = Duration.ofSeconds(30);
    private Duration retryMaxDelay = Duration.ofMinutes(30);
    private int maxAttempts = 5;
}
