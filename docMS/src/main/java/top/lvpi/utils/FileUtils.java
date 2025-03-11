package top.lvpi.utils;

import org.apache.commons.lang3.StringUtils;

public class FileUtils {
    /**
     * 从URL或路径中提取文件名
     */
    public static String extractFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        int lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlashIndex < 0 ? path : path.substring(lastSlashIndex + 1);
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex);
    }

    /**
     * 获取文件的MIME类型
     */
    public static String getContentType(String fileName) {
        // 根据文件扩展名返回对应的MIME类型
        String extension = getFileExtension(fileName).toLowerCase();
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
} 