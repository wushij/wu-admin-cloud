package com.wu.cloud.infra.job;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式任务锁（P1-3 修复）
 * <p>
 * 使用 SETNX（{@code setIfAbsent}）+ TTL 实现，保证多实例环境下同一任务同一时刻只有一个实例执行。
 * 锁 key 格式：{@code job:lock:{jobName}}，TTL 防止持锁实例宕机后死锁。
 * </p>
 */
@Slf4j
@Component
public class DistributedJobLock {

    private static final String LOCK_PREFIX = "job:lock:";
    /** 默认锁 TTL：10 分钟（需大于任务最大执行时长） */
    private static final long DEFAULT_TTL_SECONDS = 600;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /**
     * 尝试获取锁（默认 TTL 10 分钟）。
     *
     * @param jobName 任务名（全局唯一）
     * @return true=获取成功（当前实例执行任务），false=已被其他实例持有（跳过本次）
     */
    public boolean tryLock(String jobName) {
        return tryLock(jobName, DEFAULT_TTL_SECONDS);
    }

    /**
     * 尝试获取锁。
     *
     * @param jobName     任务名
     * @param ttlSeconds  锁 TTL（秒），到期自动释放
     */
    public boolean tryLock(String jobName, long ttlSeconds) {
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + jobName, "1", ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(ok)) {
            log.debug("[JobLock] 获取锁成功 job={}", jobName);
            return true;
        }
        log.debug("[JobLock] 获取锁失败（已被持有）job={}", jobName);
        return false;
    }

    /**
     * 释放锁。
     */
    public void unlock(String jobName) {
        redisTemplate.delete(LOCK_PREFIX + jobName);
    }
}
