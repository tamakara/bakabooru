package com.tamakara.bakabooru.initializer;

import com.tamakara.bakabooru.module.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagInitializer implements ApplicationRunner {

    private final SystemSettingService systemSettingService;


    @Async // 异步执行，绝不阻塞 Spring Boot 启动
    @Override
    public void run(ApplicationArguments args) {
        boolean isTagInitialized = systemSettingService.getBooleanSetting("tag.initialized");

        if (isTagInitialized) return;

        log.info("开始初始化标签向量...");

        try {
            String targetUrl = aiServiceUrl + "/api/tags/backfill";
            ResponseEntity<String> response = longTimeoutRestTemplate.postForEntity(targetUrl, null, String.class);

            // 3. 检查结果
            if (response.getStatusCode().is2xxSuccessful()) {
                // 4. 成功后更新状态表
                markAsInitialized(setting);
                log.info("🎉 标签向量回填大功告成！已将系统状态 tags_initialized 设为 true。");
            } else {
                log.warn("⚠️ AI 服务返回异常状态码: {}，稍后重启系统可再次重试。", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ 呼叫 AI 服务进行回填时发生网络异常或超时，状态位保持 false", e);
        }
    }

}