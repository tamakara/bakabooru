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

    @Bean("uploadExecutor")
    public Executor uploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("upload-");
        executor.initialize();
        return executor;
    }

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
