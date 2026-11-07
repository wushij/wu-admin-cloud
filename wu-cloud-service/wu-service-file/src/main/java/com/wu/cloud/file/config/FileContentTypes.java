package com.wu.cloud.file.config;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

/**
 * 按扩展名解析 MIME，避免 Windows 上 probeContentType 为空导致浏览器把 zip 等当文本打开。
 */
public final class FileContentTypes {

    private static final Map<String, String> BY_EXT = Map.ofEntries(
            Map.entry(".zip", "application/zip"),
            Map.entry(".rar", "application/vnd.rar"),
            Map.entry(".7z", "application/x-7z-compressed"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".txt", MediaType.TEXT_PLAIN_VALUE),
            Map.entry(".json", MediaType.APPLICATION_JSON_VALUE),
            Map.entry(".xml", MediaType.APPLICATION_XML_VALUE),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", MediaType.IMAGE_PNG_VALUE),
            Map.entry(".jpg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry(".gif", MediaType.IMAGE_GIF_VALUE),
            Map.entry(".bmp", "image/bmp"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".mov", "video/quicktime"),
            Map.entry(".avi", "video/x-msvideo"),
            Map.entry(".mp3", "audio/mpeg"),
            Map.entry(".wav", "audio/wav"),
            Map.entry(".m4a", "audio/mp4"),
            Map.entry(".ogg", "audio/ogg")
    );

    private FileContentTypes() {
    }

    public static String byFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String ext = LocalFileStorage.getSuffix(fileName).toLowerCase(Locale.ROOT);
        return BY_EXT.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /**
     * 优先按扩展名判断 MIME，避免客户端伪造 Content-Type 把 .jpg 声明成 text/html 造成存储型 XSS。
     */
    public static String resolve(String fileName, String uploadedType, String probedType) {
        String byName = byFileName(fileName);
        if (!MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(byName)) {
            return byName;
        }
        if (StringUtils.hasText(uploadedType) && !isGenericMime(uploadedType)) {
            return uploadedType;
        }
        if (StringUtils.hasText(probedType) && !isGenericMime(probedType)) {
            return probedType;
        }
        return byName;
    }

    public static boolean isGenericMime(String mime) {
        if (!StringUtils.hasText(mime)) {
            return true;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(mime)
                || MediaType.TEXT_PLAIN_VALUE.equals(mime)
                || "application/download".equals(mime);
    }

    public static boolean isInlineMime(String mime) {
        if (!StringUtils.hasText(mime)) {
            return false;
        }
        String lower = mime.toLowerCase(Locale.ROOT);
        if ("image/svg+xml".equals(lower) || lower.startsWith("text/html")) {
            return false;
        }
        return lower.startsWith("image/")
                || lower.startsWith("video/")
                || lower.startsWith("audio/")
                || "application/pdf".equals(lower);
    }

    public static boolean shouldForceDownload(String mime) {
        return !isInlineMime(mime);
    }
}

