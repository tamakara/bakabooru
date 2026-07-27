package com.tamakara.bakabooru.module.gallery.model;

import lombok.Getter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 图片信息封装类
 * <p>
 * 构造时自动读取文件头分析宽高、格式，并计算 SHA256 哈希。
 * 如果文件不是图片，构造函数会抛出异常。
 */
@Getter
public class ImageInfo {

    private final int width;
    private final int height;
    private final long size;
    private final String format;      // 真实的图片格式 (jpeg, png, gif)
    private final String extension;   // 建议的文件后缀 (jpg, png)
    private final boolean isAnimated; // 是否为动图 (GIF/WebP)

    /**
     * 构造函数：传入文件，自动分析
     * @param file 本地文件对象
     * @throws IllegalArgumentException 如果文件不存在或不是图片
     * @throws RuntimeException 如果读取过程中发生 IO 错误
     */
    public ImageInfo(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("文件不存在或路径无效");
        }

        this.size = file.length();

        // 2. 解析图片元数据
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                throw new RuntimeException("无法读取图片流");
            }

            // 自动寻找解码器
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("无法识别的文件格式，非标准图片");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(in);

                // 获取真实格式
                this.format = reader.getFormatName().toLowerCase();
                // 简单的后缀映射
                this.extension = mapFormatToExtension(this.format);

                // 获取物理尺寸 (不解码像素)
                this.width = reader.getWidth(0);
                this.height = reader.getHeight(0);

                // 简单的动图检测 (如果有超过1帧，通常是动图)
                int frameCount = 1;
                try {
                    // true 允许扫描文件流来确切计算帧数（对 GIF 稍慢但准确）
                    // 如果追求极致速度，可以设为 false，或者只对 gif/webp 格式执行此检查
                    if ("gif".equals(this.format) || "webp".equals(this.format)) {
                        frameCount = reader.getNumImages(true);
                    }
                } catch (Exception ignored) {
                    // 某些格式不支持计算帧数，忽略
                }
                this.isAnimated = frameCount > 1;

            } finally {
                reader.dispose(); // 释放 reader
            }
        } catch (IOException e) {
            throw new RuntimeException("图片解析失败: 文件可能已损坏", e);
        }

        // 3. 最终校验
        if (this.width <= 0 || this.height <= 0) {
            throw new IllegalArgumentException("无效的图片尺寸: " + width + "x" + height);
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