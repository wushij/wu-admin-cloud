package com.wu.cloud.file.controller;

import com.wu.cloud.file.config.LocalFileStorage;
import com.wu.cloud.file.service.impl.SysFileServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地文件直链访问：GET /file/** 与 GET /files/**
 * <p>兼容单体迁移数据：/api/files/yyyy/MM/dd/xxx 与 uploads/ 前缀。</p>
 */
@RestController
public class FileAccessController {

    @Resource
    private LocalFileStorage localFileStorage;

    @GetMapping({"/file/**", "/files/**"})
    public ResponseEntity<org.springframework.core.io.Resource> getFile(HttpServletRequest request) throws Exception {
        String relative = extractRelative(request.getRequestURI());
        if (!StringUtils.hasText(relative)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Path full = resolveExistingFile(relative);
        if (full == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String storageName = full.getFileName().toString();
        String probed = Files.probeContentType(full);
        String contentType = LocalFileStorage.resolveType(storageName, probed);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new FileSystemResource(full));
    }

    private Path resolveExistingFile(String relative) {
        String normalized = SysFileServiceImpl.normalizeStoragePath(relative);
        for (String candidate : buildCandidatePaths(normalized)) {
            Path full = localFileStorage.resolvePath(candidate);
            if (Files.isRegularFile(full)) {
                return full;
            }
        }
        return null;
    }

    private String[] buildCandidatePaths(String relative) {
        if (relative.startsWith("uploads/")) {
            String withoutUploads = relative.substring("uploads/".length());
            return new String[]{relative, withoutUploads};
        }
        return new String[]{relative, "uploads/" + relative};
    }

    private static String extractRelative(String uri) {
        if (uri == null) {
            return "";
        }
        int idx = uri.indexOf("/files/");
        if (idx >= 0) {
            return stripQuery(uri.substring(idx + "/files/".length()));
        }
        idx = uri.indexOf("/file/");
        if (idx >= 0) {
            return stripQuery(uri.substring(idx + "/file/".length()));
        }
        return "";
    }

    private static String stripQuery(String relative) {
        int q = relative.indexOf('?');
        if (q >= 0) {
            relative = relative.substring(0, q);
        }
        int hash = relative.indexOf('#');
        if (hash >= 0) {
            relative = relative.substring(0, hash);
        }
        return relative;
    }
}