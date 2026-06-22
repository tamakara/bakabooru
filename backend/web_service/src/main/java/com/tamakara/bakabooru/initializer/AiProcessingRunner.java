package com.tamakara.bakabooru.initializer;

import com.tamakara.bakabooru.module.ai.service.AiProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiProcessingRunner implements ApplicationRunner {

    private final AiProcessingService aiProcessingService;

    @Override
    public void run(ApplicationArguments args) {
        aiProcessingService.enqueuePendingImages();
    }
}
