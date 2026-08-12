package com.wu.cloud.file.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SysFileServiceImpl.normalizeStoragePath")
class SysFileServiceImplNormalizePathTest {

    @Test
    @DisplayName("剥离 /file/ /files/ 与前导斜杠")
    void normalizesPrefixes() {
        assertEquals("", SysFileServiceImpl.normalizeStoragePath(null));
        assertEquals("uploads/a.png", SysFileServiceImpl.normalizeStoragePath("/file/uploads/a.png"));
        assertEquals("uploads/a.png", SysFileServiceImpl.normalizeStoragePath("file/uploads/a.png"));
        assertEquals("uploads/a.png", SysFileServiceImpl.normalizeStoragePath("/files/uploads/a.png"));
        assertEquals("uploads/a.png", SysFileServiceImpl.normalizeStoragePath("/uploads/a.png"));
        assertEquals("uploads/a.png", SysFileServiceImpl.normalizeStoragePath("uploads/a.png"));
    }
}
