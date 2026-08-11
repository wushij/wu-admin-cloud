package com.wu.cloud.system.support;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录失败锁定支持（与 wu-cloud-auth 共用 Redis Key，避免 Feign 环依赖）。
 *
 * <p>Key 格式必须与 {@code com.wu.cloud.auth.support.AuthRedisKeys} 保持一致：
 * {@code auth:login:fail:{username}} / {@code auth:login:lock:{username}}</p>
 */
@Component
public class LoginLockSupport {

    private static final String LOGIN_FAIL = "auth:login:fail:%s";
    private static final String LOGIN_LOCK = "auth:login:lock:%s";

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    public void unlockUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        String name = username.trim();
        redisTemplate.delete(String.format(LOGIN_FAIL, name));
        redisTemplate.delete(String.format(LOGIN_LOCK, name));
    }

    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Long ttl = redisTemplate.getExpire(String.format(LOGIN_LOCK, username.trim()));
        return ttl != null && ttl > 0;
    }

    public long remainSeconds(String username) {
        if (username == null || username.isBlank()) {
            return 0L;
        }
        Long ttl = redisTemplate.getExpire(String.format(LOGIN_LOCK, username.trim()));
        return ttl != null && ttl > 0 ? ttl : 0L;
    }

    public int failCount(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        String val = redisTemplate.opsForValue().get(String.format(LOGIN_FAIL, username.trim()));
        if (val == null || val.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 扫描当前被锁定的用户名集合（SCAN 风格简化：keys，开发/小规模可用）。
     */
    public Set<String> listLockedUsernames() {
        Set<String> keys = redisTemplate.keys("auth:login:lock:*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>(keys.size());
        String prefix = "auth:login:lock:";
        for (String key : keys) {
            if (key != null && key.startsWith(prefix)) {
                names.add(key.substring(prefix.length()));
            }
        }
        return names;
    }

    public Map<String, LockStatus> getUserLockStatusMap(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LockStatus> map = new HashMap<>(usernames.size());
        for (String username : usernames) {
            if (username == null || username.isBlank()) {
                continue;
            }
            String name = username.trim();
            LockStatus status = new LockStatus();
            status.setLocked(isLocked(name));
            status.setRemainSeconds(remainSeconds(name));
            status.setFailCount(failCount(name));
            map.put(name, status);
        }
        return map;
    }

    public static class LockStatus {
        private boolean locked;
        private long remainSeconds;
        private int failCount;

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public long getRemainSeconds() {
            return remainSeconds;
        }

        public void setRemainSeconds(long remainSeconds) {
            this.remainSeconds = remainSeconds;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }
    }
}
