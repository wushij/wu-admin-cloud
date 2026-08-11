package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.infra.entity.OperLogDO;
import com.wu.cloud.infra.mapper.OperLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志定时清理任务（P1-3 修复）
 * <p>
 * 每天凌晨 3:00 执行，清理 180 天前的操作日志。
 * 多实例部署下通过 {@link DistributedJobLock} 保证只执行一次。
 * </p>
 */
@Slf4j
@Component
public class OperLogCleanJob {

    /** 日志保留天数 */
    private static final int RETENTION_DAYS = 180;

    @Resource
    private OperLogMapper operLogMapper;
    @Resource
    private DistributedJobLock jobLock;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpired() {
        if (!jobLock.tryLock("operLogClean")) {
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int deleted = operLogMapper.delete(
                    new LambdaQueryWrapper<OperLogDO>()
                            .lt(OperLogDO::getOperTime, deadline));
            log.info("[Job] 操作日志清理完成，删除 {} 条（{} 天前）", deleted, RETENTION_DAYS);
        } catch (Exception e) {
            log.error("[Job] 操作日志清理失败", e);
        } finally {
            jobLock.unlock("operLogClean");
        }
    }
}
