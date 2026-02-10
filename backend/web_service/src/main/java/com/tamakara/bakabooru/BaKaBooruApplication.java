package com.tamakara.bakabooru;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableJpaAuditing
public class BaKaBooruApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BaKaBooruApplication.class);

        // 创建数据目录
        app.addListeners((ApplicationEnvironmentPreparedEvent event) -> {
            Environment env = event.getEnvironment();
            String dataDir = env.getProperty("app.data-dir", "data");

            try {
                Files.createDirectories(Paths.get(dataDir));
                Files.createDirectories(Paths.get(dataDir, "image"));
                Files.createDirectories(Paths.get(dataDir, "temp"));
                Files.createDirectories(Paths.get(dataDir, "temp", "thumbnail"));
                Files.createDirectories(Paths.get(dataDir, "temp", "pending"));
                Files.createDirectories(Paths.get(dataDir, "db"));
            } catch (IOException e) {
                throw new RuntimeException("创建目录失败", e);
            }
        });

        app.run(args);
    }
}
