package com.tamakara.bakabooru.initializer;

import com.tamakara.bakabooru.module.ai.client.AiServiceClient;
import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagInitializer implements ApplicationRunner {

    private final SystemSettingService systemSettingService;
    private final AiServiceClient aiServiceClient;

    @Async
    @Override
    public void run(ApplicationArguments args) {
        boolean isTagInitialized = systemSettingService.getBooleanSetting("tag.initialized");

        if (isTagInitialized) return;

        log.info("开始初始化标签向量...");

        try {
            aiServiceClient.initTags();
            systemSettingService.updateSetting("tag.initialized", "true");
            log.info("标签向量初始化完成。");
        } catch (Exception e) {
            log.error("发生网络异常或超时", e);
        }
    }

}