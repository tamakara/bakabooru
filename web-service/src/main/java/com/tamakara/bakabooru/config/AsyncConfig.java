package com.tamakara.bakabooru.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private final AiServiceProperties aiServiceProperties;

    @Bean("aiExecutor")
    public Executor aiExecutor() {
        int concurrency = aiServiceProperties.getConcurrency();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ai-processing-");
        executor.initialize();
        return executor;
    }
}
