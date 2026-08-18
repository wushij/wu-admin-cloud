package com.wu.cloud.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebSocket Redis 广播：UTF-8 解码")
class WebSocketRedisBroadcasterTest {

    @Test
    @DisplayName("onMessage 按 UTF-8 解析中文，避免 Windows 默认 GBK 乱码")
    void onMessage_keepsChineseWhenBodyIsUtf8() throws Exception {
        WebSocketRedisBroadcaster broadcaster = new WebSocketRedisBroadcaster();
        AtomicReference<String> delivered = new AtomicReference<>();
        broadcaster.setDeliveryCallback((userId, json) -> delivered.set(json));

        String inner = "{\"type\":\"chat\",\"content\":\"怎么这样了啊\"}";
        byte[] utf8 = new ObjectMapper().writeValueAsBytes(Map.of("userId", 2, "payload", inner));

        // 对照：若按平台 GBK 解码，就会得到截图里那种「鎬庝箞…」
        Charset gbk = Charset.forName("GBK");
        assertFalse(new String(utf8, gbk).contains("怎么这样了啊"));

        Message message = new DefaultMessage("ws:channel:test".getBytes(StandardCharsets.UTF_8), utf8);
        broadcaster.onMessage(message, null);

        assertEquals(inner, delivered.get());
        assertTrue(delivered.get().contains("怎么这样了啊"));
    }
}
