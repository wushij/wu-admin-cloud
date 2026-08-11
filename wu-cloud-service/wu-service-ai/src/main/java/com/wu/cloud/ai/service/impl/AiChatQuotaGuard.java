package com.wu.cloud.ai.service.impl;

import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.core.context.UserContextHolder;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;

/**
 * AI 对话限流与 Token 配额守卫（微服务适配版）
 * <p>
 * 闸门①每用户每分钟次数（固定窗口）；闸门②每用户每日 token 配额。
 * 限流参数取自 AI 服务配置 {@code ai.quota.per-user-minute} 与 {@code ai.quota.tokens-per-user-daily}（缺省不限）；
 * 角色级配额解析依赖 system 权限体系，微服务下简化以单值兜底配置为准。
 * 全程 fail-open：任何异常（含 Redis 不可用）一律放行，仅记 warn，限流不能成为对话可用性的单点。
 * </p>
 */
@Component
public class AiChatQuotaGuard {

    private static final Logger log = LoggerFactory.getLogger(AiChatQuotaGuard.class);

    /** 分钟级限流 key 前缀（ai:rl:chat:{userId}:{minute}） */
    private static final String MINUTE_KEY_PREFIX = "ai:rl:chat:";
    /** 日 token 配额 key 前缀（ai:quota:tokens:{userId}:{yyyyMMdd}） */
    private static final String TOKEN_QUOTA_KEY_PREFIX = "ai:quota:tokens:";

    private static final String MSG_TOO_FREQUENT = "提问太频繁了，请稍候再试 🍵";
    private static final String MSG_QUOTA_EXCEEDED = "今日 AI 用量已达上限，明天再来吧";

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    @Resource
    private RedissonClient redissonClient;

    private int getMaxPerMinute() {
        return dynamicConfigHelper.getAiChatPerUserMinute();
    }

    private long getTokensPerUserDaily() {
        return dynamicConfigHelper.getAiTokensPerUserDaily();
    }

    /** 依次通过闸门①②；通过返回 null，被拒返回可回传前端的中文文案。内部完成分钟计数自增；任何异常 fail-open 放行。 */
    public String check(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }
        try {
            String freqErr = checkPerMinute(userId);
            if (freqErr != null) {
                return freqErr;
            }
            return checkDailyTokenQuota(userId);
        } catch (Exception e) {
            log.warn("AI 对话配额检查异常，fail-open 放行 user={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /** 闸门①：分钟级固定窗口计数，进门即自增 */
    private String checkPerMinute(Long userId) {
        int max = getMaxPerMinute();
        if (max <= 0) {
            return null;
        }
        long minute = System.currentTimeMillis() / 60_000L;
        RAtomicLong counter = redissonClient.getAtomicLong(MINUTE_KEY_PREFIX + userId + ":" + minute);
        long n = counter.incrementAndGet();
        if (n == 1) {
            counter.expire(Duration.ofMinutes(2));
        }
        return n > max ? MSG_TOO_FREQUENT : null;
    }

    /** 闸门②：日 token 配额（不预扣，宽进严出） */
    private String checkDailyTokenQuota(Long userId) {
        long quota = getTokensPerUserDaily();
        if (quota <= 0) {
            return null;
        }
        long used = redissonClient.getAtomicLong(tokenQuotaKey(userId)).get();
        return used >= quota ? MSG_QUOTA_EXCEEDED : null;
    }

    /** 回写当日 token 用量（onComplete 调用，成功与中断均回写）。tokens<=0 或配额关闭时空操作；任何异常仅 warn。 */
    public void recordUsage(Long userId, long totalTokens) {
        if (userId == null || userId <= 0 || totalTokens <= 0) {
            return;
        }
        try {
            if (getTokensPerUserDaily() <= 0) {
                return;
            }
            RAtomicLong counter = redissonClient.getAtomicLong(tokenQuotaKey(userId));
            counter.addAndGet(totalTokens);
            if (counter.remainTimeToLive() < 0) {
                counter.expire(Duration.ofHours(48));
            }
        } catch (Exception e) {
            log.warn("AI 对话 token 用量回写异常 user={}: {}", userId, e.getMessage());
        }
    }

    private String tokenQuotaKey(Long userId) {
        return TOKEN_QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now();
    }

    // 兼容：供引用方读取当前用户角色（实际在 AiToolExecutor 中使用 UserContextHolder 即可）
    static boolean isAdmin() {
        return UserContextHolder.getRoles() != null && UserContextHolder.getRoles().contains("super_admin");
    }
}
