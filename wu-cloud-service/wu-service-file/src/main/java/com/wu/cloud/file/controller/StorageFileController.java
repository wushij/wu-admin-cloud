package com.wu.cloud.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.file.config.FileContentTypes;
import com.wu.cloud.file.config.StorageObjectKeyBuilder;
import com.wu.cloud.file.config.StorageRoutingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * 对象存储/本地静态文件开放流式访问控制器（与单体 StorageFileController 对齐）
 * <p>无论底层存储引擎是 MinIO、OSS、COS 还是本地磁盘，均提供统一标准的只读流式访问，
 * 支持图片直接渲染、音视频 Range 断点播放与文档下载。</p>
 */
@Slf4j
@Tag(name = "对象存储文件访问")
@RestController
@RequiredArgsConstructor
public class StorageFileController {

    private final StorageRoutingService storageRoutingService;

    @Operation(summary = "多引擎文件流式读取与预览")
    @GetMapping({"/api/storage/files/{bucket}/**", "/storage/files/{bucket}/**"})
    public void getStorageFile(@PathVariable("bucket") String bucket,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        if (!storageRoutingService.isAllowedBucket(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix1 = "/api/storage/files/" + bucket + "/";
        String prefix2 = "/storage/files/" + bucket + "/";
        String rawObjectName = "";

        if (path != null) {
            if (path.startsWith(prefix1)) {
                rawObjectName = path.substring(prefix1.length());
            } else if (path.startsWith(prefix2)) {
                rawObjectName = path.substring(prefix2.length());
            }
        }

        if (!StringUtils.hasText(rawObjectName)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String objectName;
        try {
            // PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE 是未解码的原始 URI，中文/空格/百分号文件名必须显式解码
            objectName = UriUtils.decode(rawObjectName, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        while (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }

        // 以 "/" 结尾的是目录而不是文件，直接按资源不存在收口
        if (objectName.endsWith("/")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 避免目录穿越字符
        if (objectName.contains("../") || objectName.contains("..\\") || objectName.contains("..")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long size = storageRoutingService.getFileSize(bucket, objectName);
        long start = 0L;
        long end = size > 0 ? size - 1 : -1L;
        boolean partial = false;

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (size > 0 && StringUtils.hasText(rangeHeader) && rangeHeader.startsWith("bytes=")) {
            try {
                String spec = rangeHeader.substring("bytes=".length()).split(",")[0].trim();
                int dash = spec.indexOf('-');
                if (dash < 0) {
                    throw new NumberFormatException("缺少范围分隔符");
                }
                if (dash == 0) {
                    long suffixLength = Long.parseLong(spec.substring(1).trim());
                    if (suffixLength <= 0) {
                        throw new NumberFormatException("非法的后缀范围");
                    }
                    start = Math.max(0L, size - suffixLength);
                    end = size - 1;
                } else {
                    start = Long.parseLong(spec.substring(0, dash).trim());
                    String endPart = spec.substring(dash + 1).trim();
                    end = endPart.isEmpty() ? size - 1 : Long.parseLong(endPart);
                }
                if (start < 0 || start >= size || end < start) {
                    throw new NumberFormatException("范围超出文件大小");
                }
                end = Math.min(end, size - 1);
                partial = true;
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + size);
                return;
            }
        }

        long contentLength = partial ? (end - start + 1) : (size >= 0 ? size : -1L);

        InputStream inputStream = partial
                ? storageRoutingService.getFile(bucket, objectName, start, contentLength)
                : storageRoutingService.getFile(bucket, objectName);
        if (inputStream == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream in = inputStream) {
            String contentType = FileContentTypes.resolve(
                    objectName, null, URLConnection.guessContentTypeFromName(objectName));
            boolean forceDownload = FileContentTypes.shouldForceDownload(contentType);
            if (forceDownload) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                String filename = StorageObjectKeyBuilder.sanitizeFilename(objectName);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                                .toString());
            }

            response.setStatus(partial
                    ? HttpServletResponse.SC_PARTIAL_CONTENT
                    : HttpServletResponse.SC_OK);
            response.setContentType(contentType);
            if (size >= 0) {
                response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            }
            if (contentLength >= 0) {
                response.setContentLengthLong(contentLength);
            }
            if (partial) {
                response.setHeader(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + size);
            }
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader(HttpHeaders.CACHE_CONTROL,
                    objectName.startsWith("public/")
                            ? "public, max-age=86400"
                            : "private, max-age=3600");

            byte[] buffer = new byte[8192];
            long remaining = contentLength >= 0 ? contentLength : Long.MAX_VALUE;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read < 0) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, read);
                remaining -= read;
            }
            response.flushBuffer();
        } catch (Exception e) {
            log.warn("读取对象存储文件异常: bucket={}, objectName={}, reason={}",
                    bucket, objectName, e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}

