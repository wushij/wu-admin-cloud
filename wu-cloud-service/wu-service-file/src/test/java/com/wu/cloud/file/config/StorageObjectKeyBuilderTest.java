package com.wu.cloud.file.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StorageObjectKeyBuilder 四层语义路径单元测试")
class StorageObjectKeyBuilderTest {

    @Test
    @DisplayName("用户头像路径：users/{username}/avatar/avatar_{hash}.{ext}")
    void userAvatarPath() {
        String path = StorageObjectKeyBuilder.userAvatar(1001L, "admin", "my-avatar.png");
        assertTrue(path.startsWith("users/admin/avatar/avatar_"));
        assertTrue(path.endsWith(".png"));

        String fallbackPath = StorageObjectKeyBuilder.userAvatar(1002L, null, "test.jpg");
        assertTrue(fallbackPath.startsWith("users/1002/avatar/avatar_"));
        assertTrue(fallbackPath.endsWith(".jpg"));
    }

    @Test
    @DisplayName("通用文件中心路径：files/{category}/{yyyyMMdd}/{hash}_{filename}")
    void sysFilePath() {
        String imgPath = StorageObjectKeyBuilder.sysFile(null, "logo.png");
        assertTrue(imgPath.startsWith("files/images/"));
        assertTrue(imgPath.endsWith("_logo.png"));

        String docPath = StorageObjectKeyBuilder.sysFile(null, "project_summary.pdf");
        assertTrue(docPath.startsWith("files/documents/"));
        assertTrue(docPath.endsWith("_project_summary.pdf"));
    }

    @Test
    @DisplayName("聊天附件与工单路径：chat/images、chat/files、tickets")
    void chatAndTicketPaths() {
        String chatImg = StorageObjectKeyBuilder.chatImage("screenshot.png");
        assertTrue(chatImg.startsWith("chat/images/"));
        assertTrue(chatImg.endsWith("_screenshot.png"));

        String chatFile = StorageObjectKeyBuilder.chatFile("data_table.xlsx");
        assertTrue(chatFile.startsWith("chat/files/"));
        assertTrue(chatFile.endsWith("_data_table.xlsx"));

        String ticketPath = StorageObjectKeyBuilder.ticketAttachment(12L, "bug.png");
        assertTrue(ticketPath.startsWith("tickets/12/"));
        assertTrue(ticketPath.endsWith("_bug.png"));
    }

    @Test
    @DisplayName("流式访问 URL：/api/storage/files/{bucket}/{objectKey}")
    void storageFileUrl() {
        String url = StorageObjectKeyBuilder.storageFileUrl("wu-admin", "users/admin/avatar/avatar_123.jpg");
        assertEquals("/api/storage/files/wu-admin/users/admin/avatar/avatar_123.jpg", url);
    }
}

