package com.wu.cloud.file.config;

import cn.hutool.core.util.StrUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 对象存储客户端（MinIO SDK 统一封装）。
 *
 * <p>MinIO SDK 走标准 S3 协议，因此同一套代码可驱动：
 * MinIO 自建、阿里云 OSS（S3 兼容 endpoint）、腾讯云 COS（S3 兼容 endpoint）。
 * 引擎差异只体现在 endpoint / region / 域名与桶名配置上。</p>
 */
@Slf4j
public class ObjectStorageClient {

    private final StorageConfig config;
    private final MinioClient client;

    public ObjectStorageClient(StorageConfig config) {
        this.config = config;
        this.client = buildClient(config);
    }

    private MinioClient buildClient(StorageConfig config) {
        String endpoint = resolveEndpoint(config);
        if (StrUtil.isBlank(endpoint)) {
            throw new IllegalArgumentException("对象存储 endpoint 未配置");
        }
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(resolveAccessKey(config), resolveSecretKey(config));
        String region = resolveRegion(config);
        if (StrUtil.isNotBlank(region)) {
            builder.region(region);
        }
        return builder.build();
    }

    /** 上传并返回可访问 URL */
    public String upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        String bucket = resolveBucket(config);
        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, size, -1L);
        if (StrUtil.isNotBlank(contentType)) {
            builder.contentType(contentType);
        }
        client.putObject(builder.build());
        return buildAccessUrl(bucket, objectName);
    }

    public InputStream get(String objectName) throws Exception {
        return client.getObject(GetObjectArgs.builder()
                .bucket(resolveBucket(config))
                .object(objectName)
                .build());
    }

    public InputStream get(String objectName, long offset, long length) throws Exception {
        GetObjectArgs.Builder builder = GetObjectArgs.builder()
                .bucket(resolveBucket(config))
                .object(objectName);
        if (offset >= 0) {
            builder.offset(offset);
        }
        if (length > 0) {
            builder.length(length);
        }
        return client.getObject(builder.build());
    }

    public byte[] getBytes(String objectName) throws Exception {
        try (InputStream input = get(objectName)) {
            if (input == null) {
                return new byte[0];
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    public long getSize(String objectName) {
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                    .bucket(resolveBucket(config))
                    .object(objectName)
                    .build());
            return stat == null ? -1L : stat.size();
        } catch (Exception e) {
            log.debug("对象存储读取大小失败 object={} reason={}", objectName, e.getMessage());
            return -1L;
        }
    }

    public void delete(String objectName) throws Exception {
        client.removeObject(RemoveObjectArgs.builder()
                .bucket(resolveBucket(config))
                .object(objectName)
                .build());
    }

    /** 连通性测试：成功返回 null，失败返回原因 */
    public String testConnectionDetail() {
        try {
            String bucket = resolveBucket(config);
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            return exists ? null : "存储桶不存在: " + bucket;
        } catch (Exception e) {
            return "对象存储连接失败: " + e.getMessage();
        }
    }

    public String buildAccessUrl(String bucket, String objectName) {
        String domain = StrUtil.firstNonBlank(
                config.getMinioDomain(), config.getCosDomain(), config.getOssDomain());
        if (StrUtil.isNotBlank(domain)) {
            return StrUtil.removeSuffix(domain.trim(), "/") + "/" + objectName;
        }
        return StorageObjectKeyBuilder.storageFileUrl(bucket, objectName);
    }

    public String storageType() {
        return StrUtil.nullToEmpty(config.getType()).toLowerCase();
    }

    private String resolveEndpoint(StorageConfig config) {
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        return switch (type) {
            case "oss" -> config.getOssEndpoint();
            case "cos" -> StrUtil.isNotBlank(config.getCosRegion())
                    ? "https://" + config.getCosBucket() + ".cos." + config.getCosRegion() + ".myqcloud.com"
                    : null;
            default -> config.getMinioEndpoint();
        };
    }

    private String resolveAccessKey(StorageConfig config) {
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        return switch (type) {
            case "oss" -> config.getOssAccessKeyId();
            case "cos" -> config.getCosSecretId();
            default -> config.getMinioAccessKey();
        };
    }

    private String resolveSecretKey(StorageConfig config) {
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        return switch (type) {
            case "oss" -> config.getOssAccessKeySecret();
            case "cos" -> config.getCosSecretKey();
            default -> config.getMinioSecretKey();
        };
    }

    private String resolveBucket(StorageConfig config) {
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        return switch (type) {
            case "oss" -> config.getOssBucket();
            case "cos" -> config.getCosBucket();
            default -> config.getMinioBucket();
        };
    }

    private String resolveRegion(StorageConfig config) {
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        return "cos".equals(type) ? config.getCosRegion() : null;
    }
}
