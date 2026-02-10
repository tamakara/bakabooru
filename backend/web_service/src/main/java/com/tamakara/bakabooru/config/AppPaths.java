package com.tamakara.bakabooru.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Getter
public class AppPaths {

    private final Path dataDir;
    private final Path imageDir;
    private final Path tempDir;
    private final Path thumbnailDir;
    private final Path pendingDir;
    private final Path dbDir;

    public AppPaths(@Value("${app.data-dir}") String dataRoot) {
        Path root = Paths.get(dataRoot).toAbsolutePath().normalize();

        this.dataDir = root;
        this.imageDir = root.resolve("image");
        this.tempDir = root.resolve("temp");
        this.thumbnailDir = tempDir.resolve("thumbnail");
        this.pendingDir = tempDir.resolve("pending");
        this.dbDir = root.resolve("db");
    }
}
