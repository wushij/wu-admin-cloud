package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.service.LoginLockService;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败锁定服务实现（Redis 计数）
 */
@Service
public class LoginLockServiceImpl implements LoginLockService {

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private AuthConfigLoader authConfigLoader;

    @Override
    public void assertNotLocked(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        Long ttl = redisTemplate.getExpire(AuthRedisKeys.loginLock(username));
        if (ttl != null && ttl > 0) {
            long minutes = Math.max(1, ttl / 60);
            throw new BusinessException("账号已被锁定，请 " + minutes + " 分钟后重试");
        }
    }

    @Override
    public int recordFailure(String username) {
        int max = authConfigLoader.getMaxRetryCount();
        long lockSeconds = authConfigLoader.getLockTimeSeconds();
        if (username == null || username.isBlank()) {
            return max;
        }
        String key = AuthRedisKeys.loginFail(username);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return max;
        }
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(lockSeconds));
        }
        if (count >= max) {
            redisTemplate.opsForValue().set(
                    AuthRedisKeys.loginLock(username),
                    "1",
                    Duration.ofSeconds(lockSeconds));
            redisTemplate.delete(key);
            return 0;
        }
        return (int) (max - count);
    }

    @Override
    public void clearFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        redisTemplate.delete(AuthRedisKeys.loginFail(username));
        redisTemplate.delete(AuthRedisKeys.loginLock(username));
    }
}
