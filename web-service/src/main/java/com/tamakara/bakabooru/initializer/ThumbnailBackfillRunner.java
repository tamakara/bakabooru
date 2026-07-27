package com.tamakara.bakabooru.initializer;

import com.tamakara.bakabooru.module.image.service.ImageUrlService;
import com.tamakara.bakabooru.module.image.service.StorageService;
import com.tamakara.bakabooru.module.image.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final StorageService storageService;
    private final ThumbnailService thumbnailService;
    private final ImageUrlService imageUrlService;

    @Override
    public void run(ApplicationArguments args) {
        Thread worker = new Thread(this::backfillMissingThumbnails, "thumbnail-backfill");
        worker.setDaemon(true);
        worker.start();
    }

    private void backfillMissingThumbnails() {
        List<String> hashes = jdbcTemplate.queryForList("SELECT hash FROM images ORDER BY id ASC", String.class);
        int created = 0;
        for (String hash : hashes) {
            File original = null;
            try {
                String thumbnailObject = imageUrlService.getThumbnailObjectName(hash);
                if (storageService.existFile(thumbnailObject)) {
                    continue;
                }
                original = storageService.getFile("original/" + hash);
                thumbnailService.generateAndUploadThumbnail(original, hash);
                created++;
            } catch (Exception e) {
                log.warn("历史缩略图生成失败 hash={}: {}", hash, e.getMessage());
            } finally {
                if (original != null && original.exists()) {
                    original.delete();
                }
            }
        }
        if (created > 0) {
            log.info("历史缩略图补齐完成，新增 {} 个", created);
        }
    }
}
