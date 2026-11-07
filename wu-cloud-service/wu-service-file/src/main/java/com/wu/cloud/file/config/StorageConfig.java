package com.wu.cloud.file.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全局存储引擎配置参数模型（支持 local / minio / oss / cos，与单体新版 StorageConfig 对齐）。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StorageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 激活的存储类型：local / minio / cos / oss */
    @Builder.Default
    private String type = "local";

    /** 本地存储根目录 */
    private String localPath;

    /** 本地公网访问域名（可选，留空走相对路径） */
    private String localDomain;

    // === MinIO ===
    private String minioDomain;
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioSecretKey;
    @Builder.Default
    private String minioBucket = "wu-admin";

    // === 腾讯云 COS（S3 兼容通道）===
    private String cosSecretId;
    private String cosSecretKey;
    private String cosRegion;
    private String cosBucket;
    private String cosDomain;

    // === 阿里云 OSS（S3 兼容通道）===
    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucket;
    private String ossDomain;
}
