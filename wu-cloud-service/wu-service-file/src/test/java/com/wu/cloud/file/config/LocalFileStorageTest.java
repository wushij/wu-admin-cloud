package com.wu.cloud.file.config;

import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LocalFileStorage 单元测试")
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileStorageProperties properties;
    @Mock
    private DynamicConfigHelper dynamicConfigHelper;

    @InjectMocks
    private LocalFileStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.getLocalBaseDir()).thenReturn(tempDir.toString());
        when(properties.getMaxSize()).thenReturn(50 * 1024 * 1024L);
        when(properties.getAllowedExtensions()).thenReturn("");
        when(dynamicConfigHelper.getFileMaxSizeMb()).thenReturn(10);
        when(dynamicConfigHelper.getFileAllowedExtensions()).thenReturn("");
        storage.init();
    }

    @Test
    @DisplayName("空文件拒绝")
    void validate_empty_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.validateUpload("a.png", 0));
        assertEquals("文件内容为空", ex.getMessage());
    }

    @Test
    @DisplayName("超大小拒绝")
    void validate_tooLarge_throws() {
        long over = 11L * 1024 * 1024;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.validateUpload("a.png", over));
        assertTrue(ex.getMessage().contains("文件超过大小限制"));
    }

    @Test
    @DisplayName("文件名路径穿越拒绝")
    void validate_pathTraversalName_throws() {
        assertThrows(BusinessException.class, () -> storage.validateUpload("../a.png", 10));
        assertThrows(BusinessException.class, () -> storage.validateUpload("a/b.png", 10));
        assertThrows(BusinessException.class, () -> storage.validateUpload("a\\b.png", 10));
    }

    @Test
    @DisplayName("黑名单后缀无条件拒绝")
    void validate_deniedExtension_throws() {
        for (String name : new String[]{"x.html", "x.svg", "x.js", "x.jsp", "x.exe"}) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> storage.validateUpload(name, 10));
            assertTrue(ex.getMessage().contains("禁止上传"), name);
        }
    }

    @Test
    @DisplayName("白名单外后缀拒绝")
    void validate_notInWhitelist_throws() {
        when(dynamicConfigHelper.getFileAllowedExtensions()).thenReturn("png,jpg");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.validateUpload("doc.pdf", 10));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    @DisplayName("合法 png 通过校验")
    void validate_png_ok() {
        when(dynamicConfigHelper.getFileAllowedExtensions()).thenReturn("png,jpg");
        assertDoesNotThrow(() -> storage.validateUpload("avatar.png", 1024));
    }

    @Test
    @DisplayName("resolvePath 阻止目录穿越")
    void resolvePath_blocksTraversal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.resolvePath("../../etc/passwd"));
        assertEquals("非法文件路径", ex.getMessage());
    }

    @Test
    @DisplayName("upload 写入并返回 URL，穿越路径拒绝")
    void upload_writesAndRejectsEscape() throws Exception {
        String url = storage.upload(
                new ByteArrayInputStream("hi".getBytes(StandardCharsets.UTF_8)),
                "uploads/2026", "a.png");
        assertEquals("/api/files/uploads/2026/a.png", url);
        assertTrue(Files.isRegularFile(tempDir.resolve("uploads/2026/a.png")));

        // fileName 含 .. 时 normalize 后可能逃出子目录但仍在 basePath 内；
        // 用绝对逃逸 storagePath 验证 basePath 守卫
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.upload(new ByteArrayInputStream("x".getBytes()), "../escape", "a.png"));
        assertEquals("非法文件路径", ex.getMessage());
    }

    @Test
    @DisplayName("getSuffix / resolveType")
    void suffixAndType() {
        assertEquals(".png", LocalFileStorage.getSuffix("a.PNG"));
        assertEquals("", LocalFileStorage.getSuffix("noext"));
        assertEquals("image/png", LocalFileStorage.resolveType("a.png", null));
        assertEquals("application/pdf", LocalFileStorage.resolveType("a.pdf", null));
        assertEquals("text/plain", LocalFileStorage.resolveType("a.bin", "text/plain"));
    }
}
