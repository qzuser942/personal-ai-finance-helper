package com.finance.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 文件工具类
 *
 * @author 胡宪棋
 */
public class FileUtil {

    /** 允许的图片文件类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    ));

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "webp"
    ));

    /** 最大文件大小 10MB */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 校验图片文件类型
     */
    public static boolean isValidImageType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    /**
     * 校验图片文件扩展名
     */
    public static boolean isValidExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    /**
     * 生成UUID文件名
     */
    public static String generateFileName(String originalFilename) {
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    /**
     * 创建目录（递归）
     */
    public static void createDirs(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    /**
     * 获取文件大小（字节）
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 获取目录下所有文件列表
     */
    public static File[] listFiles(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            return dir.listFiles();
        }
        return new File[0];
    }

    /**
     * 计算目录总大小
     */
    public static long getDirSize(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        File[] files = dir.listFiles();
        if (files == null) return 0;
        long total = 0;
        for (File f : files) {
            total += f.length();
        }
        return total;
    }
}
