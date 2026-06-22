package com.tamakara.bakabooru.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-service")
public class AiServiceProperties {
    private String url;
    private int concurrency = 10;
}
