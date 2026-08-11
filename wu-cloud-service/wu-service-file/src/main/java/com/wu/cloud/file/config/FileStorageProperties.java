package com.wu.cloud.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储本地配置
 */
@Configuration
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    /** 本地存储根目录 */
    private String localBaseDir = "./data/upload";

    /** 上传大小上限（字节），默认 50MB */
    private long maxSize = 50 * 1024 * 1024L;

    /** 允许的文件后缀，空表示不限制 */
    private String allowedExtensions = "";

    public String getLocalBaseDir() {
        return localBaseDir;
    }

    public void setLocalBaseDir(String localBaseDir) {
        this.localBaseDir = localBaseDir;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public String getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
