package com.wu.cloud.api.file.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 存储引擎配置 DTO（系统服务 ↔ 文件服务内部调用）。
 * <p>字段与单体 StorageConfigVO 对齐：type/local/minio/cos/oss 四类引擎参数，
 * 另含上传限制（maxSizeMb、allowedExtensions），统一存放在 sys_config_group 的 file 分组。</p>
 */
@Data
public class StorageConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 引擎类型：local / minio / oss / cos */
    private String type;
    private String localPath;
    private String localDomain;

    private String minioDomain;
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioSecretKey;
    private String minioBucket;

    private String cosRegion;
    private String cosBucket;
    private String cosSecretId;
    private String cosSecretKey;
    private String cosDomain;

    private String ossEndpoint;
    private String ossBucket;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossDomain;

    /** 单文件大小上限（MB） */
    private Integer maxSizeMb;
    /** 允许的扩展名（逗号分隔） */
    private String allowedExtensions;
}
