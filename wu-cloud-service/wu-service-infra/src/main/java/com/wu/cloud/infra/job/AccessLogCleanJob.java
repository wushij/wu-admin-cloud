package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.mapper.AccessLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 访问日志定时清理任务（P1-3 修复）
 * <p>
 * 每天凌晨 2:30 执行，清理 90 天前的 API 访问日志。
 * 多实例部署下通过 {@link DistributedJobLock} 保证只执行一次。
 * </p>
 */
@Slf4j
@Component
public class AccessLogCleanJob {

    /** 日志保留天数 */
    private static final int RETENTION_DAYS = 90;

    @Resource
    private AccessLogMapper accessLogMapper;
    @Resource
    private DistributedJobLock jobLock;

    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanExpired() {
        if (!jobLock.tryLock("accessLogClean")) {
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int deleted = accessLogMapper.delete(
                    new LambdaQueryWrapper<AccessLogDO>()
                            .lt(AccessLogDO::getAccessTime, deadline));
            log.info("[Job] 访问日志清理完成，删除 {} 条（{} 天前）", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("[Job] 访问日志清理失败", e);
        } finally {
            jobLock.unlock("accessLogClean");
        }
    }
}
