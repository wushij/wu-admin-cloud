package com.wu.cloud.pay.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.api.order.feign.RemoteOrderService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.redis.service.RedisService;
import com.wu.cloud.pay.entity.PayOutboxMessageDO;
import com.wu.cloud.pay.mapper.PayOutboxMessageMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 支付结果通知本地消息表补偿任务（Transactional Outbox 投递器）。
 * <p>
 * 每 30 秒轮询 pay_outbox_message 中 status=PENDING 且 next_retry_time≤now 的记录，
 * 通过类型化 OpenFeign 契约投递到订单服务（携带 TraceId/身份上下文、受 Sentinel 熔断保护）。
 * 成功 → SUCCESS；失败 → 指数退避重试（30s × 2^retry），达到 MAX_RETRY → DEAD 供人工介入。
 * 多实例部署下通过 Redisson 分布式锁保证只执行一次。
 * </p>
 */
@Slf4j
@Component
public class PayOutboxCompensateJob {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 100;
    private static final String LOCK_KEY = "pay:outbox:compensate";

    @Resource
    private PayOutboxMessageMapper payOutboxMessageMapper;
    @Resource
    private RemoteOrderService remoteOrderService;
    @Resource
    private RedisService redisService;

    @Scheduled(fixedDelay = 30000)
    public void compensate() {
        RLock lock = redisService.getLock(LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 60, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            List<PayOutboxMessageDO> pending = payOutboxMessageMapper.selectList(
                    new LambdaQueryWrapper<PayOutboxMessageDO>()
                            .eq(PayOutboxMessageDO::getStatus, PayOutboxMessageDO.STATUS_PENDING)
                            .le(PayOutboxMessageDO::getNextRetryTime, LocalDateTime.now())
                            .last("LIMIT " + BATCH_SIZE));
            if (pending.isEmpty()) {
                return;
            }
            log.info("[PayOutbox] 待投递消息 {} 条", pending.size());
            for (PayOutboxMessageDO message : pending) {
                processOne(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[PayOutbox] 补偿任务异常", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void processOne(PayOutboxMessageDO message) {
        try {
            R<Boolean> resp = remoteOrderService.markPaidByOrderNo(message.getOrderNo(), message.getPayType());
            if (resp != null && resp.isSuccess()) {
                payOutboxMessageMapper.update(null, new LambdaUpdateWrapper<PayOutboxMessageDO>()
                        .eq(PayOutboxMessageDO::getId, message.getId())
                        .set(PayOutboxMessageDO::getStatus, PayOutboxMessageDO.STATUS_SUCCESS)
                        .set(PayOutboxMessageDO::getUpdateTime, LocalDateTime.now()));
                log.info("[PayOutbox] 投递成功 id={} bizKey={}", message.getId(), message.getBizKey());
            } else {
                markRetry(message, resp == null ? "无响应" : resp.getMsg());
            }
        } catch (Exception e) {
            markRetry(message, e.getMessage());
        }
    }

    private void markRetry(PayOutboxMessageDO message, String error) {
        int retryCount = (message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1;
        if (retryCount >= MAX_RETRY) {
            payOutboxMessageMapper.update(null, new LambdaUpdateWrapper<PayOutboxMessageDO>()
                    .eq(PayOutboxMessageDO::getId, message.getId())
                    .set(PayOutboxMessageDO::getStatus, PayOutboxMessageDO.STATUS_DEAD)
                    .set(PayOutboxMessageDO::getRetryCount, retryCount)
                    .set(PayOutboxMessageDO::getUpdateTime, LocalDateTime.now()));
            log.error("[PayOutbox] 达到最大重试次数，标记 DEAD id={} bizKey={} err={}",
                    message.getId(), message.getBizKey(), error);
            return;
        }
        // 指数退避：30s * 2^retry
        long delaySec = 30L * (1L << retryCount);
        payOutboxMessageMapper.update(null, new LambdaUpdateWrapper<PayOutboxMessageDO>()
                .eq(PayOutboxMessageDO::getId, message.getId())
                .set(PayOutboxMessageDO::getRetryCount, retryCount)
                .set(PayOutboxMessageDO::getNextRetryTime, LocalDateTime.now().plusSeconds(delaySec))
                .set(PayOutboxMessageDO::getUpdateTime, LocalDateTime.now()));
        log.warn("[PayOutbox] 投递失败，{}s 后重试 id={} retry={} err={}",
                delaySec, message.getId(), retryCount, error);
    }
}
