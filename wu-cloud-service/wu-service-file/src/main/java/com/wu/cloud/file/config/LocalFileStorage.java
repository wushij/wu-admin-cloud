package com.wu.cloud.file.config;

import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wu.cloud.file.service.FileStorageService;

/**
 * 轻量本地文件存储（替代旧版 LocalFileStorage / FileContentTypes）
 * 提供上传、读取、删除能力，文件按 storagePath 落到本地磁盘根目录
 */
@Component
public class LocalFileStorage implements FileStorageService {

    @Resource
    private FileStorageProperties properties;

    @Resource
    private com.wu.cloud.common.config.DynamicConfigHelper dynamicConfigHelper;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(properties.getLocalBaseDir()).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
    }

    /**
     * 始终禁止的高危后缀（黑名单，优先级高于白名单配置）。
     *
     * <p>P1-4 修复：{@code allowed-extensions} 默认值为空字符串，语义是「不限制」，
     * 也就是默认允许上传 .jsp/.html/.exe 等文件。配合静态资源目录暴露时，
     * 上传一个 .html 即可构成存储型 XSS，上传 .jsp 在部分容器下甚至可能被解析执行。
     * 因此这里增加一道<b>无条件生效</b>的黑名单兜底，即便运维忘记配白名单也安全。</p>
     */
    private static final Set<String> DENIED_EXTENSIONS = Set.of(
            ".jsp", ".jspx", ".jspf", ".php", ".php3", ".php4", ".php5", ".phtml",
            ".asp", ".aspx", ".ascx", ".ashx", ".asmx", ".cer",
            ".exe", ".dll", ".bat", ".cmd", ".com", ".msi", ".scr",
            ".sh", ".bash", ".jar", ".war", ".py", ".rb", ".pl",
            ".html", ".htm", ".xhtml", ".svg", ".vbs", ".js"
    );

    public void validateUpload(String fileName, long size) {
        if (size <= 0) {
            throw new BusinessException("文件内容为空");
        }
        long maxBytes = dynamicConfigHelper.getFileMaxSizeMb() * 1024L * 1024L;
        if (maxBytes <= 0) maxBytes = properties.getMaxSize();
        if (size > maxBytes) {
            throw new BusinessException("文件超过大小限制（最大 " + dynamicConfigHelper.getFileMaxSizeMb() + "MB）");
        }
        // 文件名本身不得包含路径分隔符或穿越片段
        if (fileName != null && (fileName.contains("/") || fileName.contains("\\") || fileName.contains(".."))) {
            throw new BusinessException("非法文件名");
        }
        String ext = getSuffix(fileName);
        // 1) 黑名单：无条件生效
        if (DENIED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("出于安全考虑，禁止上传该类型文件: " + ext);
        }
        // 2) 白名单：优先从数据库读，fallback 到 yml
        String allowedExt = dynamicConfigHelper.getFileAllowedExtensions();
        if (allowedExt == null || allowedExt.isBlank()) {
            allowedExt = properties.getAllowedExtensions();
        }
        if (allowedExt != null && !allowedExt.isBlank()) {
            Set<String> allowed = Stream.of(allowedExt.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .map(s -> s.startsWith(".") ? s : "." + s)
                    .collect(Collectors.toSet());
            if (!allowed.contains(ext)) {
                throw new BusinessException("不支持的文件类型: " + ext);
            }
        }
    }

    /**
     * 上传文件流到指定存储路径
     *
     * @return 可访问的文件 URL（基于上下文路径 /file/**）
     */
    public String upload(InputStream input, String storagePath, String fileName) throws IOException {
        Path dir = resolvePath(storagePath);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName).normalize();
        if (!target.startsWith(basePath)) {
            throw new BusinessException("非法存储路径");
        }
        Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/api/files/" + storagePath + "/" + fileName;
    }

    public byte[] readBytes(String storagePath) throws IOException {
        Path existing = resolveExistingPath(storagePath);
        if (existing != null) {
            return Files.readAllBytes(existing);
        }
        return Files.readAllBytes(resolvePath(storagePath));
    }

    /**
     * 解析已存在的本地文件路径（兼容相对路径与 uploads/ 前缀回退）
     */
    public Path resolveExistingPath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return null;
        }
        Path direct = resolvePath(storagePath);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        String clean = storagePath.replace("\\", "/");
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (clean.startsWith("uploads/")) {
            Path withoutUploads = resolvePath(clean.substring("uploads/".length()));
            if (Files.isRegularFile(withoutUploads)) {
                return withoutUploads;
            }
        } else {
            Path withUploads = resolvePath("uploads/" + clean);
            if (Files.isRegularFile(withUploads)) {
                return withUploads;
            }
        }
        return null;
    }

    /**
     * 打开本地文件输入流（不存在时返回 null）
     */
    public InputStream openStream(String storagePath) {
        Path target = resolveExistingPath(storagePath);
        if (target == null) {
            return null;
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 打开本地文件区间输入流（用于 Range 断点续传）
     */
    public InputStream openStream(String storagePath, long offset, long length) {
        InputStream in = openStream(storagePath);
        if (in == null) {
            return null;
        }
        try {
            if (offset > 0) {
                long skipped = in.skip(offset);
                while (skipped < offset) {
                    long step = in.skip(offset - skipped);
                    if (step <= 0) {
                        if (in.read() < 0) {
                            break;
                        }
                        step = 1;
                    }
                    skipped += step;
                }
            }
            return in;
        } catch (IOException e) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
            return null;
        }
    }

    public Path resolvePath(String storagePath) {
        Path p = basePath.resolve(Paths.get(storagePath)).normalize();
        if (!p.startsWith(basePath)) {
            throw new BusinessException("非法文件路径");
        }
        return p;
    }

    public void delete(String storagePath) throws IOException {
        Path p = resolvePath(storagePath);
        Files.deleteIfExists(p);
    }

    public static String getSuffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx).toLowerCase() : "";
    }

    /**
     * 根据文件名/contentType 粗略解析文件类型（替代旧版 FileContentTypes）
     */
    public static String resolveType(String fileName, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String ext = getSuffix(fileName).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp" -> "image/" + (ext.isEmpty() ? "png" : ext.substring(1));
            case ".mp4", ".avi", ".mov", ".webm" -> "video/mp4";
            case ".mp3", ".wav", ".ogg" -> "audio/mpeg";
            case ".pdf" -> "application/pdf";
            case ".zip", ".rar", ".7z" -> "application/zip";
            case ".doc", ".docx" -> "application/msword";
            case ".xls", ".xlsx" -> "application/vnd.ms-excel";
            default -> "application/octet-stream";
        };
    }
}
