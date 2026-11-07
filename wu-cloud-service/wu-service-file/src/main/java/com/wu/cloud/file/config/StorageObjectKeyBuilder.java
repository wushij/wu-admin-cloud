package com.wu.cloud.file.config;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 对象存储 ObjectKey 规范化生成工具类（与单体 StorageObjectKeyBuilder 对齐）
 * <p>按「业务模块 -> 归属对象 -> 分类/日期 -> 唯一资源」四层语义建立清晰链路，
 * 避免无意义散乱 Hash 与根路径堆积，方便 MinIO / OSS / COS 控制台统一治理与排查。</p>
 *
 * <pre>
 * 典型存储链路形态：
 * 1. 头像：       users/{username}/avatar/avatar_{shortHash}.{ext}
 * 2. 通用文件：   files/{category}/{yyyyMMdd}/{shortHash}_{filename}
 * 3. 聊天附件：   chat/{category}/{yyyyMMdd}/{shortHash}_{filename}
 * 4. 工单附件：   tickets/{ticketId}/{shortHash}_{filename}
 * 5. 知识库附件： ai-knowledge/{knowledgeId}/{shortHash}_{filename}
 * 6. 导出报表：   export/{type}/{yyyyMMdd}/{shortHash}_{filename}
 * 7. 公共资源：   public/{category}/{shortHash}_{filename}
 * </pre>
 */
public final class StorageObjectKeyBuilder {

    public static final String DEFAULT_BUCKET = "wu-admin";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StorageObjectKeyBuilder() {
    }

    /**
     * 规范化拼装相对路径：去除首尾多余斜杠，防止路径穿越
     */
    public static String build(String relativePath) {
        String cleanPath = relativePath != null ? relativePath.trim().replace("\\", "/") : "";
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath.contains("..")) {
            cleanPath = cleanPath.replace("..", "");
        }
        return cleanPath;
    }

    /**
     * 用户头像路径：{@code users/{username}/avatar/avatar_{shortHash}.{ext}}
     */
    public static String userAvatar(Long userId, String username, String originalFilename) {
        String userSegment = StringUtils.hasText(username)
                ? sanitizeSegment(username)
                : (userId != null && userId > 0 ? String.valueOf(userId) : "user");
        String suffix = safeSuffix(originalFilename);
        if (!StringUtils.hasText(suffix)) {
            suffix = ".jpg";
        }
        String fileName = "avatar_" + shortHash() + suffix;
        return build("users/" + userSegment + "/avatar/" + fileName);
    }

    /**
     * 通用业务文件中心路径：{@code files/{category}/{yyyyMMdd}/{shortHash}_{cleanFilename}}
     */
    public static String sysFile(String customCategory, String originalFilename) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String category = StringUtils.hasText(customCategory)
                ? sanitizeSegment(customCategory)
                : resolveCategory(originalFilename);
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("files/" + category + "/" + date + "/" + fileName);
    }

    /**
     * 企业 IM 聊天图片路径：{@code chat/images/{yyyyMMdd}/{shortHash}_{cleanFilename}}
     */
    public static String chatImage(String originalFilename) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("chat/images/" + date + "/" + fileName);
    }

    /**
     * 企业 IM 聊天通用附件路径：{@code chat/files/{yyyyMMdd}/{shortHash}_{cleanFilename}}
     */
    public static String chatFile(String originalFilename) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("chat/files/" + date + "/" + fileName);
    }

    /**
     * 工单系统附件路径：{@code tickets/{ticketId}/{shortHash}_{cleanFilename}}
     */
    public static String ticketAttachment(Long ticketId, String originalFilename) {
        String ticketSegment = (ticketId != null && ticketId > 0) ? String.valueOf(ticketId) : "general";
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("tickets/" + ticketSegment + "/" + fileName);
    }

    /**
     * AI 知识库文档路径：{@code ai-knowledge/{knowledgeId}/{shortHash}_{cleanFilename}}
     */
    public static String aiKnowledgeDocument(Long knowledgeId, String originalFilename) {
        String kbSegment = (knowledgeId != null && knowledgeId > 0) ? String.valueOf(knowledgeId) : "general";
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("ai-knowledge/" + kbSegment + "/" + fileName);
    }

    /**
     * 数据导出文件路径：{@code export/{exportType}/{yyyyMMdd}/{shortHash}_{cleanFilename}}
     */
    public static String exportFile(String exportType, String originalFilename) {
        String typeSegment = StringUtils.hasText(exportType) ? sanitizeSegment(exportType) : "data";
        String date = LocalDate.now().format(DATE_FORMATTER);
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return build("export/" + typeSegment + "/" + date + "/" + fileName);
    }

    /**
     * 公共静态资源路径（免认证）：{@code public/{category}/{shortHash}_{cleanFilename}}
     */
    public static String publicResource(String category, String originalFilename) {
        String cat = StringUtils.hasText(category) ? sanitizeSegment(category) : "common";
        String cleanName = sanitizeFilename(originalFilename);
        String fileName = shortHash() + "_" + cleanName;
        return "public/" + cat + "/" + fileName;
    }

    /**
     * 生成统一可通过 StorageFileController 访问的流式访问 URL
     */
    public static String storageFileUrl(String bucketName, String objectKey) {
        String bucket = StringUtils.hasText(bucketName) ? bucketName.trim() : DEFAULT_BUCKET;
        String key = objectKey != null ? objectKey.trim() : "";
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        String encodedKey = UriUtils.encodePath(key, StandardCharsets.UTF_8);
        return "/api/storage/files/" + bucket + "/" + encodedKey;
    }

    /**
     * 生成 16 位紧凑随机 Hash
     */
    public static String shortHash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 文件名安全消毒。
     * <p>对象 key 只保留 ASCII 安全字符，原始文件名仍完整保存在数据库 original_name 中用于展示。</p>
     */
    public static String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        String clean = filename.replace("\\", "/");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        clean = clean.trim();
        if (!StringUtils.hasText(clean)) {
            return "file";
        }

        String suffix = safeSuffix(clean);
        String base = suffix.isEmpty() ? clean : clean.substring(0, clean.length() - suffix.length());
        base = base.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._]+", "")
                .replaceAll("[._]+$", "");
        if (!StringUtils.hasText(base)) {
            base = "file";
        }
        if (base.length() > 90) {
            base = base.substring(0, 90);
        }
        return base + suffix;
    }

    private static String safeSuffix(String filename) {
        String suffix = getSuffix(filename).toLowerCase(Locale.ROOT);
        return suffix.matches("^\\.[a-z0-9]{1,10}$") ? suffix : "";
    }

    /**
     * 路径段安全过滤，转小写并去除非法字符
     */
    public static String sanitizeSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "default";
        }
        String clean = segment.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9._-]", "_");
        while (clean.startsWith(".") || clean.startsWith("_")) {
            clean = clean.substring(1);
        }
        return StringUtils.hasText(clean) ? clean : "default";
    }

    /**
     * 根据文件名后缀推导所属业务分类
     */
    public static String resolveCategory(String filename) {
        String ext = getSuffix(filename).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico" -> "images";
            case ".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".webm" -> "videos";
            case ".mp3", ".wav", ".flac", ".aac", ".m4a", ".ogg" -> "audios";
            case ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf", ".txt", ".md", ".csv" -> "documents";
            case ".zip", ".rar", ".7z", ".tar", ".gz" -> "archives";
            default -> "others";
        };
    }

    public static String getSuffix(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : "";
    }
}

