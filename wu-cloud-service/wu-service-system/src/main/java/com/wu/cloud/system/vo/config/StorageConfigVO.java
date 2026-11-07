package com.wu.cloud.system.vo.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 存储引擎配置展示 VO（对应前端 StorageConfigVO）。
 * <p>密钥类字段不回传明文，仅给出「是否已配置」标记；写入时若不携带密钥则保留原值。</p>
 */
@Data
public class StorageConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 引擎类型：local / minio / oss / cos */
    private String type;
    private String localPath;
    private String localDomain;

    private String minioDomain;
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioBucket;
    /** MinIO 密钥是否已配置（不回传明文） */
    private Boolean minioSecretKeyConfigured;

    private String cosRegion;
    private String cosBucket;
    private String cosSecretId;
    private String cosDomain;
    /** COS 密钥是否已配置（不回传明文） */
    private Boolean cosSecretKeyConfigured;

    private String ossEndpoint;
    private String ossBucket;
    private String ossAccessKeyId;
    private String ossDomain;
    /** OSS AccessKeySecret 是否已配置（不回传明文） */
    private Boolean ossAccessKeySecretConfigured;

    /** 单文件大小上限（MB） */
    private Integer maxSizeMb;
    /** 允许的扩展名（逗号分隔） */
    private String allowedExtensions;

    /** 当前生效的引擎类型 */
    private String activeType;
    /** 当前生效引擎的桶名 */
    private String activeBucket;
    /** 当前配置是否可用（必填项齐全） */
    private Boolean healthy;
}
