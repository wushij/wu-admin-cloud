package com.wu.cloud.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * WebSocket Redis 广播器（P1-2 修复）
 * <p>
 * 解决多实例部署下 WebSocket 会话孤岛问题：
 * <ul>
 *   <li>用户在线状态存 Redis（{@code ws:online:{userId} → instanceId}），跨实例可查</li>
 *   <li>私聊消息通过 Redis Pub/Sub 路由到目标用户所在实例的频道</li>
 *   <li>全员广播通过 Redis Pub/Sub 分发到所有实例</li>
 * </ul>
 * 每个实例启动时生成唯一 instanceId，订阅自己的频道 {@code ws:channel:{instanceId}}
 * 和全局广播频道 {@code ws:broadcast}。
 */
@Slf4j
@Component
public class WebSocketRedisBroadcaster implements MessageListener {

    /** 在线状态 key：ws:online:{userId} = instanceId */
    private static final String ONLINE_KEY = "ws:online:";
    /** 单实例投递频道：ws:channel:{instanceId} */
    private static final String CHANNEL_PREFIX = "ws:channel:";
    /** 全局广播频道 */
    private static final String BROADCAST_CHANNEL = "ws:broadcast";
    /** 在线 key TTL，需由客户端心跳续期 */
    private static final long ONLINE_TTL_SECONDS = 300;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String instanceId = UUID.randomUUID().toString();

    /** 收到跨实例消息后的回调：(userId, json) → 本地投递；userId=null 表示全局广播 */
    private volatile BiConsumer<Long, String> deliveryCallback;

    public void setDeliveryCallback(BiConsumer<Long, String> callback) {
        this.deliveryCallback = callback;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @PostConstruct
    public void init() {
        redisMessageListenerContainer.addMessageListener(this,
                new ChannelTopic(CHANNEL_PREFIX + instanceId));
        redisMessageListenerContainer.addMessageListener(this,
                new ChannelTopic(BROADCAST_CHANNEL));
        log.info("[WebSocket] Redis 广播器初始化完成 instanceId={}", instanceId);
    }

    /** 标记用户上线（本实例） */
    public void setUserOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_KEY + userId, instanceId,
                ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 心跳续期 */
    public void refreshOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_KEY + userId, instanceId,
                ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 标记用户下线（仅当仍是本实例时才删除，避免误删迁移中的 key） */
    public void setUserOffline(Long userId) {
        String current = redisTemplate.opsForValue().get(ONLINE_KEY + userId);
        if (instanceId.equals(current)) {
            redisTemplate.delete(ONLINE_KEY + userId);
        }
    }

    /** 查询用户是否在线（跨实例） */
    public boolean isUserOnline(Long userId) {
        Boolean exists = redisTemplate.hasKey(ONLINE_KEY + userId);
        return Boolean.TRUE.equals(exists);
    }

    /** 发送给指定用户（自动路由到目标实例） */
    public void sendToUser(Long userId, String json) {
        String target = redisTemplate.opsForValue().get(ONLINE_KEY + userId);
        if (target == null) {
            return; // 用户不在线，消息丢弃（已由 DB 持久化，离线时可拉历史）
        }
        try {
            String msg = objectMapper.writeValueAsString(new ChannelMsg(userId, json));
            redisTemplate.convertAndSend(CHANNEL_PREFIX + target, msg);
        } catch (Exception e) {
            log.error("[WebSocket] sendToUser 发布失败 userId={}", userId, e);
        }
    }

    /** 全局广播（所有实例） */
    public void broadcast(String json) {
        try {
            String msg = objectMapper.writeValueAsString(new ChannelMsg(null, json));
            redisTemplate.convertAndSend(BROADCAST_CHANNEL, msg);
        } catch (Exception e) {
            log.error("[WebSocket] broadcast 发布失败", e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            ChannelMsg msg = objectMapper.readValue(body, ChannelMsg.class);
            BiConsumer<Long, String> cb = this.deliveryCallback;
            if (cb != null) {
                cb.accept(msg.userId(), msg.payload());
            }
        } catch (Exception e) {
            log.warn("[WebSocket] Redis 频道消息解析失败", e);
        }
    }

    /** 频道消息体：userId=null 表示全局广播 */
    private record ChannelMsg(Long userId, String payload) {
    }
}
