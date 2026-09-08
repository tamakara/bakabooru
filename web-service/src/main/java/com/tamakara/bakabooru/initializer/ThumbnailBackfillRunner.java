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
                if (!storageService.existFile("original/" + hash)) {
                    jdbcTemplate.update("UPDATE images SET image_status = 'MISSING' WHERE hash = ?", hash);
                    log.warn("闁告鍠庡ù妯肩磽閸濆嫨浜奸柨娑樿嫰閸戯繝寮介崶顏嶅敹濞?MISSING hash={}", hash);
                    continue;
                }
                if (storageService.existFile(thumbnailObject)) {
                    jdbcTemplate.update("UPDATE images SET image_status = CASE WHEN image_status IN ('MISSING', 'ANALYZING', 'ERROR') THEN image_status ELSE 'NORMAL' END WHERE hash = ?", hash);
                    continue;
                }
                original = storageService.getFile("original/" + hash);
                thumbnailService.generateAndUploadThumbnail(original, hash);
                jdbcTemplate.update("UPDATE images SET image_status = CASE WHEN image_status IN ('MISSING', 'ANALYZING', 'ERROR') THEN image_status ELSE 'NORMAL' END WHERE hash = ?", hash);
                created++;
            } catch (Exception e) {
                log.warn("闁告ê妫楄ぐ鍓佺磽閳哄啯娈ｉ柛銉ュ⒔閺佹捇骞嬮幇顑句杭閻?hash={}: {}", hash, e.getMessage());
            } finally {
                if (original != null && original.exists()) {
                    original.delete();
                }
            }
        }
        if (created > 0) {
            log.info("闁告ê妫楄ぐ鍓佺磽閳哄啯娈ｉ柛銉﹀礃钘熷缁樺姇閻ｎ剟骞嬮幇鍓佺闁哄倹婢橀·?{} 濞?, created");
        }
    }
}
