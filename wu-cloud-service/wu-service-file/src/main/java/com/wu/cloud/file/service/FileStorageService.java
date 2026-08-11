package com.wu.cloud.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 统一文件存储服务抽象接口 (支持 本地 / OSS / S3 扩充)
 */
public interface FileStorageService {

    void validateUpload(String fileName, long size);

    String upload(InputStream input, String storagePath, String fileName) throws IOException;

    byte[] readBytes(String storagePath) throws IOException;

    Path resolvePath(String storagePath);

    void delete(String storagePath) throws IOException;
}
