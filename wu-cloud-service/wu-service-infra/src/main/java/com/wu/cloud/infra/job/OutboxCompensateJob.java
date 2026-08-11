package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.infra.entity.OutboxDO;
import com.wu.cloud.infra.mapper.OutboxMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表补偿任务（P1-4 修复）
 * <p>
 * 每 30 秒轮询 infra_outbox 表中 status=PENDING 且 next_retry_time≤now 的记录，
 * 通过 @LoadBalanced RestTemplate 调用目标服务 API。
 * 成功 → SUCCESS，失败 → 指数退避重试，达到 MAX_RETRY → DEAD。
 * 多实例部署下通过 {@link DistributedJobLock} 保证只执行一次。
 * </p>
 */
@Slf4j
@Component
public class OutboxCompensateJob {

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 100;

    @Resource
    private OutboxMapper outboxMapper;
    @Resource
    private DistributedJobLock jobLock;
    @Resource
    private RestTemplate restTemplate;

    @Scheduled(fixedDelay = 30000)
    public void compensate() {
        if (!jobLock.tryLock("outboxCompensate")) {
            return;
        }
        try {
            List<OutboxDO> pending = outboxMapper.selectList(
                    new LambdaQueryWrapper<OutboxDO>()
                            .eq(OutboxDO::getStatus, "PENDING")
                            .le(OutboxDO::getNextRetryTime, LocalDateTime.now())
                            .last("LIMIT " + BATCH_SIZE));
            if (pending.isEmpty()) {
                return;
            }
            log.info("[Outbox] 待补偿消息 {} 条", pending.size());
            for (OutboxDO msg : pending) {
                processOne(msg);
            }
        } catch (Exception e) {
            log.error("[Outbox] 补偿任务异常", e);
        } finally {
            jobLock.unlock("outboxCompensate");
        }
    }

    private void processOne(OutboxDO msg) {
        try {
            String url = "http://" + msg.getTargetService() + msg.getTargetApi();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(msg.getPayload(), headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                msg.setStatus("SUCCESS");
                msg.setUpdateTime(LocalDateTime.now());
                outboxMapper.updateById(msg);
                log.info("[Outbox] 补偿成功 id={} bizKey={}", msg.getId(), msg.getBizKey());
            } else {
                markRetry(msg, "HTTP " + resp.getStatusCode());
            }
        } catch (Exception e) {
            markRetry(msg, e.getMessage());
        }
    }

    private void markRetry(OutboxDO msg, String error) {
        msg.setRetryCount(msg.getRetryCount() + 1);
        if (msg.getRetryCount() >= MAX_RETRY) {
            msg.setStatus("DEAD");
            log.error("[Outbox] 达到最大重试次数，标记 DEAD id={} bizKey={} err={}",
                    msg.getId(), msg.getBizKey(), error);
        } else {
            // 指数退避：30s * 2^retry
            long delaySec = 30L * (1L << msg.getRetryCount());
            msg.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySec));
            log.warn("[Outbox] 补偿失败，{}s 后重试 id={} retry={} err={}",
                    delaySec, msg.getId(), msg.getRetryCount(), error);
        }
        msg.setUpdateTime(LocalDateTime.now());
        outboxMapper.updateById(msg);
    }
}
