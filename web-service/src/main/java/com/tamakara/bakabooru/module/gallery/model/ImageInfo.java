package com.tamakara.bakabooru.module.gallery.model;

import lombok.Getter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/** 读取图片尺寸、格式和动画帧信息。 */
@Getter
public class ImageInfo {

    private final int width;
    private final int height;
    private final long size;
    private final String format;      // 图片格式（jpeg、png、gif）。
    private final String extension;   // 标准文件扩展名。
    private final boolean isAnimated; // 是否为动画图片。

    /**
     * 读取图片元数据。
     * @param file 图片文件
     */
    public ImageInfo(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("图片文件不存在或不可读");
        }

        this.size = file.length();

        // 使用 ImageIO 读取格式和尺寸。
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                throw new RuntimeException("无法创建图片输入流");
            }

            // 根据文件内容选择图片读取器。
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("不支持的图片格式");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);

                this.format = reader.getFormatName().toLowerCase();
                this.extension = mapFormatToExtension(this.format);
                this.width = reader.getWidth(0);
                this.height = reader.getHeight(0);
                int frameCount = 1;
                try {
                    if ("gif".equals(this.format) || "webp".equals(this.format)) {
                        frameCount = reader.getNumImages(true);
                    }
                } catch (Exception ignored) {
                    // 某些格式无法读取帧数时按单帧处理。
                }
                this.isAnimated = frameCount > 1;

            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("读取图片信息失败: " + e.getMessage(), e);
        }

        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException("图片尺寸无效: " + width + "x" + height);
        }
    }

    private String mapFormatToExtension(String formatName) {
        return switch (formatName) {
            case "jpeg" -> "jpg";
            case "wbmp" -> "bmp";
            default -> formatName;
        };
    }
}
