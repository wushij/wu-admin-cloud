package com.wu.cloud.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 消息处理器（P1-2 修复：Redis 广播）
 * <p>
 * 本实例仅维护「连接到本实例」的会话（{@link #localSessions}），跨实例路由通过
 * {@link WebSocketRedisBroadcaster} 的 Redis Pub/Sub 完成：
 * <ul>
 *   <li>sendToUser → 查 Redis 定位目标实例 → 发布到目标实例频道</li>
 *   <li>broadcast  → 发布到全局广播频道，所有实例订阅</li>
 *   <li>isOnline   → 查 Redis 在线状态 key</li>
 * </ul>
 * 验收：起两个 message 实例，A 连实例 1、B 连实例 2，互发消息能收到。
 * </p>
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /** 本实例的会话表（userId → session），仅本实例连接的用户 */
    private final Map<Long, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private WebSocketRedisBroadcaster broadcaster;

    @PostConstruct
    public void init() {
        // 注册回调：收到跨实例 Redis 消息后投递到本实例的本地会话
        broadcaster.setDeliveryCallback((userId, json) -> {
            if (userId == null) {
                // 全局广播
                localSessions.values().forEach(s -> sendRaw(s, json));
            } else {
                WebSocketSession session = localSessions.get(userId);
                if (session != null && session.isOpen()) {
                    sendRaw(session, json);
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        WebSocketSession old = localSessions.put(userId, session);
        closeQuietly(old);
        broadcaster.setUserOnline(userId);
        sendJson(session, Map.of("type", "connected", "content", "ok"));
        broadcastPresence(userId, true);
        log.info("WS connected userId={}, online(local)={}", userId, localSessions.size());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            var node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            if ("ping".equals(type)) {
                // 心跳续期：客户端 ping 时续期 Redis 在线 key（TTL 300s）
                Long uid = getUserId(session);
                if (uid != null) {
                    broadcaster.refreshOnline(uid);
                }
                sendJson(session, Map.of("type", "pong"));
                return;
            }
            if ("typing".equals(type)) {
                Long fromUserId = getUserId(session);
                long toUserId = node.path("toUserId").asLong(0);
                if (fromUserId != null && toUserId > 0 && !fromUserId.equals(toUserId)) {
                    sendTypingPayload(toUserId, fromUserId);
                }
            }
        } catch (Exception e) {
            log.warn("WS message parse failed", e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            localSessions.remove(userId, session);
            if (!localSessions.containsKey(userId)) {
                broadcaster.setUserOffline(userId);
                broadcastPresence(userId, false);
            }
        }
    }

    // ===== 供业务层调用的推送方法（签名不变，内部走 Redis 广播）=====

    public void sendToUser(Long userId, String json) {
        // 本实例有会话则直接投递（与既有规范对齐，避免 Redis 绕一圈）；跨实例才走 Pub/Sub
        WebSocketSession local = localSessions.get(userId);
        if (local != null && local.isOpen()) {
            sendRaw(local, json);
            return;
        }
        broadcaster.sendToUser(userId, json);
    }

    public void sendNotice(Long userId, Long announceId, String title, String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "notice");
            payload.put("title", title != null ? title : "");
            payload.put("content", content != null ? content : "");
            payload.put("time", System.currentTimeMillis());
            if (announceId != null) {
                payload.put("announceId", announceId);
            }
            String json = objectMapper.writeValueAsString(payload);
            if (userId == null) {
                broadcaster.broadcast(json);
            } else {
                sendToUser(userId, json);
            }
        } catch (Exception e) {
            log.error("sendNotice failed", e);
        }
    }

    public void sendChatPayload(Long receiverId, Map<String, Object> payload) {
        try {
            payload.put("type", "chat");
            payload.put("time", System.currentTimeMillis());
            sendToUser(receiverId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendChatPayload failed", e);
        }
    }

    public void sendGroupChatPayload(Long userId, Map<String, Object> payload) {
        try {
            if (!payload.containsKey("type")) {
                payload.put("type", "groupChat");
            }
            payload.put("time", System.currentTimeMillis());
            sendToUser(userId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendGroupChatPayload failed", e);
        }
    }

    public void sendTypingPayload(Long toUserId, Long fromUserId) {
        sendTypingPayload(toUserId, fromUserId, true);
    }

    public void sendTypingStopPayload(Long toUserId, Long fromUserId) {
        sendTypingPayload(toUserId, fromUserId, false);
    }

    private void sendTypingPayload(Long toUserId, Long fromUserId, boolean active) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "typing");
            payload.put("fromUserId", fromUserId);
            payload.put("active", active);
            payload.put("time", System.currentTimeMillis());
            sendToUser(toUserId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("sendTypingPayload failed", e);
        }
    }

    public boolean isOnline(Long userId) {
        return broadcaster.isUserOnline(userId);
    }

    // ===== 内部方法 =====

    private void broadcastPresence(Long userId, boolean online) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "presence",
                    "userId", userId,
                    "online", online
            ));
            broadcaster.broadcast(json);
        } catch (Exception e) {
            log.warn("broadcastPresence failed userId={}", userId, e);
        }
    }

    private void sendJson(WebSocketSession session, Map<String, ?> map) {
        try {
            sendRaw(session, objectMapper.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("WS sendJson failed userId={}", getUserId(session), e);
        }
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (session == null || !session.isOpen() || json == null) {
            return;
        }
        // WebSocketSession 非线程安全：HTTP 回调 / Redis 监听 / ping-pong 可能并发写
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException | IllegalStateException e) {
                log.warn("WS send failed", e);
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object v = session.getAttributes().get("userId");
        if (v instanceof Long l) {
            return l;
        }
        if (v != null) {
            try {
                return Long.parseLong(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }
}
