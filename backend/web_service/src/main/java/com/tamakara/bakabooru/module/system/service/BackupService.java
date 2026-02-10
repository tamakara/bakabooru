package com.tamakara.bakabooru.module.system.service;

import com.tamakara.bakabooru.module.file.service.StorageService;
import com.tamakara.bakabooru.module.gallery.entity.Image;
import com.tamakara.bakabooru.module.gallery.repository.ImageRepository;
import com.tamakara.bakabooru.module.tag.entity.Tag;
import com.tamakara.bakabooru.module.tag.repository.TagRepository;
import com.tamakara.bakabooru.config.AppPaths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final ImageRepository imageRepository;
    private final TagRepository tagRepository;
    private final SystemSettingService systemSettingService;
    private final StorageService storageService;
    private final AppPaths appPaths;

    public File createBackup() throws IOException {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        File backupFile = appPaths.getTempDir().resolve("backup_" + timestamp + ".zip").toFile();

        try (FileOutputStream fos = new FileOutputStream(backupFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 备份数据库
            File dbFile = appPaths.getDbDir().toFile();
            if (dbFile.exists()) {
                addToZip(dbFile, "db/" + dbFile.getName(), zos);
            }

            // 备份图片
            File images = appPaths.getImageDir().toFile();
            if (images.exists() && images.isDirectory()) {
                addDirectoryToZip(images, "images", zos);
            }
        }

        return backupFile;
    }

    @Transactional
    public void resetSystem() throws IOException {
        imageRepository.deleteAll();
        tagRepository.deleteAll();
        storageService.clearCache();
        systemSettingService.resetSettings();

        File images = appPaths.getImageDir().toFile();
        if (images.exists()) {
            FileUtils.cleanDirectory(images);
        }
    }

    public void restoreBackup(MultipartFile file) throws IOException {
        // 解压到临时目录
        Path tempExtractDir = appPaths.getTempDir().resolve("restore_" + System.currentTimeMillis());
        Files.createDirectories(tempExtractDir);

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(tempExtractDir.toFile(), zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // 修复 Windows 创建的归档文件
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }

        // 恢复逻辑
        File dbFile = appPaths.getDbDir().toFile();
        File restoredDb = new File(tempExtractDir.toFile(), "db/" + dbFile.getName());

        if (restoredDb.exists()) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + restoredDb.getAbsolutePath())) {
                // 1. 恢复设置
                restoreSettings(conn);

                // 2. 恢复图片
                restoreImages(conn, tempExtractDir.toFile());
            } catch (SQLException e) {
                log.error("Failed to read restored database", e);
                throw new IOException("Failed to read restored database", e);
            }
        }

        // 清理
        FileUtils.deleteDirectory(tempExtractDir.toFile());
    }

    private void restoreSettings(Connection conn) throws SQLException {
        Map<String, String> settings = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT setting_key, setting_value FROM system_settings")) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        if (!settings.isEmpty()) {
            systemSettingService.updateSettings(settings);
        }
    }

    private void restoreImages(Connection conn, File tempDir) throws SQLException, IOException {
        String sql = "SELECT * FROM images";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String hash = rs.getString("hash");
                if (imageRepository.findByHash(hash).isPresent()) {
                    continue;
                }

                Image image = new Image();
                image.setTitle(rs.getString("title"));
                image.setFileName(rs.getString("file_name"));
                image.setExtension(rs.getString("extension"));
                image.setSize(rs.getLong("size"));
                image.setWidth(rs.getInt("width"));
                image.setHeight(rs.getInt("height"));
                image.setHash(hash);

                // 尝试解析时间，如果失败则使用当前时间
                // SQLite 默认存储格式可能不同，这里简单处理，如果需要精确还原需根据实际格式解析
                // image.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));

                long oldId = rs.getLong("id");
                Set<Tag> tags = getTagsForImage(conn, oldId);
                image.setTags(tags);

                imageRepository.save(image);

                File sourceFile = new File(tempDir, "images/" + hash);
                if (sourceFile.exists()) {
                    File destFile = appPaths.getImageDir().resolve(hash).toFile();
                    FileUtils.copyFile(sourceFile, destFile);
                }
            }
        }
    }

    private Set<Tag> getTagsForImage(Connection conn, long oldImageId) throws SQLException {
        Set<Tag> tags = new HashSet<>();
        String sql = "SELECT t.name FROM tags t JOIN image_tags it ON t.id = it.tag_id WHERE it.image_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, oldImageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String tagName = rs.getString("name");
                    Tag tag = tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(tagName);
                                newTag.setType("通用");
                                return tagRepository.save(newTag);
                            });
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private void addToZip(File file, String fileName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private void addDirectoryToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                addDirectoryToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }
            addToZip(file, parentFolder + "/" + file.getName(), zos);
        }
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
