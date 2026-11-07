package com.wu.cloud.file.config;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.common.config.DynamicConfigHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储路由门面（与单体 RoutingFileStorageService 对齐）。
 *
 * <p>根据系统配置 file 分组（优先）或本地 {@code app.file.*} 配置选择存储引擎：
 * local / minio / oss / cos。写操作落到当前引擎；读/删按文件记录中的 storageType 路由，
 * 保证热切换存储引擎后历史文件仍可读取与正确删除。</p>
 */
@Slf4j
@Component
public class StorageRoutingService {

    /** 存储引擎配置与上传限制同属 file 分组（与单体 SystemConfigHelper.GROUP_FILE 一致） */
    private static final String GROUP_STORAGE = "file";
    private static final String TYPE_LOCAL = "local";

    @Resource
    private LocalFileStorage localFileStorage;

    @Resource
    private FileStorageProperties fileStorageProperties;

    @Resource
    private ObjectProvider<DynamicConfigHelper> dynamicConfigHelperProvider;

    /** 按引擎类型缓存对象存储客户端，避免每次请求重建 DNS/连接池 */
    private final Map<String, ObjectStorageClient> clientCache = new ConcurrentHashMap<>();

    /** 当前生效的存储类型 */
    public String currentType() {
        String type = resolveConfig().getType();
        return StrUtil.isBlank(type) ? TYPE_LOCAL : type.trim().toLowerCase();
    }

    /** 当前是否本地存储 */
    public boolean isLocal() {
        return TYPE_LOCAL.equals(currentType());
    }

    /** 判断文件记录中的存储类型是否为本地 */
    public boolean isLocalStorageType(String storageType) {
        return TYPE_LOCAL.equals(normalize(storageType));
    }

    /**
     * 上传文件。
     *
     * @param objectName 相对对象名，例如 files/images/20261107/hash_logo.png
     * @return 上传结果（存储类型 + 访问 URL + 桶名）
     */
    public UploadResult upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        StorageConfig config = resolveConfig();
        String type = StrUtil.nullToEmpty(config.getType()).toLowerCase();
        if (TYPE_LOCAL.equals(type) || StrUtil.isBlank(type)) {
            int idx = objectName.lastIndexOf('/');
            String dir = idx > 0 ? objectName.substring(0, idx) : "";
            String fileName = idx >= 0 ? objectName.substring(idx + 1) : objectName;
            localFileStorage.upload(inputStream, dir, fileName);
            String bucket = StorageObjectKeyBuilder.DEFAULT_BUCKET;
            String url = StrUtil.isNotBlank(config.getLocalDomain())
                    ? StrUtil.removeSuffix(config.getLocalDomain().trim(), "/") + "/" + objectName
                    : StorageObjectKeyBuilder.storageFileUrl(bucket, objectName);
            return new UploadResult(TYPE_LOCAL, url, bucket);
        }
        ObjectStorageClient client = clientFor(config);
        String url = client.upload(objectName, inputStream, size, contentType);
        return new UploadResult(type, url, bucketOf(config, type));
    }

    /**
     * 判断 URL 中的 bucket 是否属于系统允许的存储桶，防止公开代理被用于读取任意桶。
     */
    public boolean isAllowedBucket(String bucketName) {
        if (StrUtil.isBlank(bucketName)) {
            return false;
        }
        String bucket = bucketName.trim();
        if ("wu-admin".equalsIgnoreCase(bucket) || "local".equalsIgnoreCase(bucket)) {
            return true;
        }
        StorageConfig config = resolveConfig();
        return bucket.equals(StrUtil.nullToEmpty(config.getMinioBucket()).trim())
                || bucket.equals(StrUtil.nullToEmpty(config.getCosBucket()).trim())
                || bucket.equals(StrUtil.nullToEmpty(config.getOssBucket()).trim());
    }

    /**
     * 流式读取文件（优先当前引擎，未命中时回退到本地或其它已配置引擎，兼容热切换）
     */
    public InputStream getFile(String bucketName, String objectName) {
        String current = currentType();
        if (TYPE_LOCAL.equals(current)) {
            InputStream local = localFileStorage.openStream(objectName);
            if (local != null) {
                return local;
            }
        } else {
            try {
                InputStream remote = clientForType(current).get(objectName);
                if (remote != null) {
                    return remote;
                }
            } catch (Exception ignored) {
            }
        }
        // 跨引擎回退：先尝试本地磁盘，再尝试已配置的对象存储
        InputStream fallbackLocal = localFileStorage.openStream(objectName);
        if (fallbackLocal != null) {
            return fallbackLocal;
        }
        for (String candidate : new String[]{"minio", "oss", "cos"}) {
            if (candidate.equals(current)) {
                continue;
            }
            try {
                InputStream stream = clientForType(candidate).get(objectName);
                if (stream != null) {
                    return stream;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 区间流式读取文件（用于 Range 断点续传）
     */
    public InputStream getFile(String bucketName, String objectName, long offset, long length) {
        String current = currentType();
        if (TYPE_LOCAL.equals(current)) {
            InputStream local = localFileStorage.openStream(objectName, offset, length);
            if (local != null) {
                return local;
            }
        } else {
            try {
                InputStream remote = clientForType(current).get(objectName, offset, length);
                if (remote != null) {
                    return remote;
                }
            } catch (Exception ignored) {
            }
        }
        InputStream fallbackLocal = localFileStorage.openStream(objectName, offset, length);
        if (fallbackLocal != null) {
            return fallbackLocal;
        }
        for (String candidate : new String[]{"minio", "oss", "cos"}) {
            if (candidate.equals(current)) {
                continue;
            }
            try {
                InputStream stream = clientForType(candidate).get(objectName, offset, length);
                if (stream != null) {
                    return stream;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 获取文件大小（优先当前引擎，未命中回退本地及其它引擎；未知返回 -1）
     */
    public long getFileSize(String bucketName, String objectName) {
        long s = size(currentType(), objectName);
        if (s >= 0) {
            return s;
        }
        long localSize = size(TYPE_LOCAL, objectName);
        if (localSize >= 0) {
            return localSize;
        }
        for (String candidate : new String[]{"minio", "oss", "cos"}) {
            long remoteSize = size(candidate, objectName);
            if (remoteSize >= 0) {
                return remoteSize;
            }
        }
        return -1L;
    }

    /** 按记录中的存储类型读取文件字节 */
    public byte[] readBytes(String storageType, String objectName) {
        String type = normalize(storageType);
        if (TYPE_LOCAL.equals(type)) {
            try {
                return localFileStorage.readBytes(objectName);
            } catch (Exception e) {
                throw new IllegalStateException("读取本地文件失败: " + e.getMessage(), e);
            }
        }
        try {
            return clientForType(type).getBytes(objectName);
        } catch (Exception e) {
            throw new IllegalStateException("读取对象存储文件失败: " + e.getMessage(), e);
        }
    }

    /** 按记录中的存储类型删除文件 */
    public void delete(String storageType, String objectName) {
        String type = normalize(storageType);
        try {
            if (TYPE_LOCAL.equals(type)) {
                localFileStorage.delete(objectName);
            } else {
                clientForType(type).delete(objectName);
            }
        } catch (Exception e) {
            log.warn("删除文件失败 type={} object={} reason={}", type, objectName, e.getMessage());
        }
    }

    /** 按记录中的存储类型读取文件大小；未知返回 -1 */
    public long size(String storageType, String objectName) {
        String type = normalize(storageType);
        if (TYPE_LOCAL.equals(type)) {
            try {
                java.nio.file.Path existing = localFileStorage.resolveExistingPath(objectName);
                return existing != null ? java.nio.file.Files.size(existing) : -1L;
            } catch (Exception e) {
                return -1L;
            }
        }
        try {
            return clientForType(type).getSize(objectName);
        } catch (Exception e) {
            return -1L;
        }
    }

    /** 针对指定配置做连通性测试（不切换当前引擎） */
    public String testConnectionDetail(StorageConfig config) {
        if (config == null) {
            return "存储配置对象不能为空";
        }
        String type = normalize(config.getType());
        try {
            if (TYPE_LOCAL.equals(type)) {
                return null;
            }
            return new ObjectStorageClient(config).testConnectionDetail();
        } catch (Exception e) {
            return "存储引擎初始化失败: " + e.getMessage();
        }
    }

    /**
     * 热重载：清空对象存储客户端缓存，使配置变更后立即生效
     * （密钥、桶名、域名变更后旧客户端仍持有旧连接）
     */
    public void reload() {
        clientCache.clear();
        log.info("[Storage] 已重载存储客户端缓存，当前类型={}", currentType());
    }

    /** 当前生效配置（供配置页展示与诊断） */
    public StorageConfig currentConfig() {
        return resolveConfig();
    }

    /**
     * 解析存储配置：系统配置 file 分组优先，本地配置兜底。
     */
    public StorageConfig resolveConfig() {
        StorageConfig config = StorageConfig.builder()
                .type(TYPE_LOCAL)
                .localPath(fileStorageProperties.getLocalBaseDir())
                .build();
        DynamicConfigHelper helper = dynamicConfigHelperProvider.getIfAvailable();
        if (helper == null) {
            return config;
        }
        try {
            config.setType(StrUtil.blankToDefault(helper.getStr(GROUP_STORAGE, "type", "local"), "local"));
            config.setLocalPath(StrUtil.blankToDefault(
                    helper.getStr(GROUP_STORAGE, "localPath", ""), fileStorageProperties.getLocalBaseDir()));
            config.setLocalDomain(helper.getStr(GROUP_STORAGE, "localDomain", ""));

            config.setMinioEndpoint(helper.getStr(GROUP_STORAGE, "minioEndpoint", ""));
            config.setMinioAccessKey(helper.getStr(GROUP_STORAGE, "minioAccessKey", ""));
            config.setMinioSecretKey(helper.getStr(GROUP_STORAGE, "minioSecretKey", ""));
            config.setMinioBucket(StrUtil.blankToDefault(helper.getStr(GROUP_STORAGE, "minioBucket", ""), "wu-admin"));
            config.setMinioDomain(helper.getStr(GROUP_STORAGE, "minioDomain", ""));

            config.setCosSecretId(helper.getStr(GROUP_STORAGE, "cosSecretId", ""));
            config.setCosSecretKey(helper.getStr(GROUP_STORAGE, "cosSecretKey", ""));
            config.setCosRegion(helper.getStr(GROUP_STORAGE, "cosRegion", ""));
            config.setCosBucket(helper.getStr(GROUP_STORAGE, "cosBucket", ""));
            config.setCosDomain(helper.getStr(GROUP_STORAGE, "cosDomain", ""));

            config.setOssEndpoint(helper.getStr(GROUP_STORAGE, "ossEndpoint", ""));
            config.setOssAccessKeyId(helper.getStr(GROUP_STORAGE, "ossAccessKeyId", ""));
            config.setOssAccessKeySecret(helper.getStr(GROUP_STORAGE, "ossAccessKeySecret", ""));
            config.setOssBucket(helper.getStr(GROUP_STORAGE, "ossBucket", ""));
            config.setOssDomain(helper.getStr(GROUP_STORAGE, "ossDomain", ""));
        } catch (Exception e) {
            log.warn("读取系统存储配置失败，使用本地配置: {}", e.getMessage());
        }
        return config;
    }

    private ObjectStorageClient clientFor(StorageConfig config) {
        String type = normalize(config.getType());
        return clientCache.computeIfAbsent(type, key -> new ObjectStorageClient(config.toBuilder().type(key).build()));
    }

    /** 按历史记录类型构造客户端：复用当前配置中的密钥与桶，仅切换引擎类型 */
    private ObjectStorageClient clientForType(String type) {
        return clientCache.computeIfAbsent(type, key -> new ObjectStorageClient(resolveConfig().toBuilder().type(key).build()));
    }

    private String bucketOf(StorageConfig config, String type) {
        return switch (type) {
            case "oss" -> config.getOssBucket();
            case "cos" -> config.getCosBucket();
            default -> config.getMinioBucket();
        };
    }

    private String normalize(String storageType) {
        return StrUtil.isBlank(storageType) ? TYPE_LOCAL : storageType.trim().toLowerCase();
    }

    /** 上传结果 */
    public record UploadResult(String storageType, String url, String bucketName) {
    }
}
