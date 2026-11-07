package com.wu.cloud.infra.job;

import com.wu.cloud.api.system.feign.RemoteLogService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 操作日志定时清理任务。
 * <p>
 * 操作日志属主为 wu-service-system（sys_oper_log）。本任务作为调度方，
 * 每天凌晨 3:00 通过 OpenFeign 远程触发属主服务在其本地库内执行清理，
 * 多实例部署下通过 {@link DistributedJobLock} 保证只执行一次。
 * </p>
 */
@Slf4j
@Component
public class OperLogCleanJob {

    /** 日志保留天数 */
    private static final int RETENTION_DAYS = 180;

    @Resource
    private RemoteLogService remoteLogService;

    @Resource
    private DistributedJobLock jobLock;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpired() {
        if (!jobLock.tryLock("operLogClean")) {
            return;
        }
        try {
            R<Integer> resp = remoteLogService.purgeExpiredOperLogs(RETENTION_DAYS);
            if (resp != null && resp.isSuccess()) {
                log.info("[Job] 操作日志清理完成，删除 {} 条（{} 天前，由 wu-service-system 执行）",
                        resp.getData(), RETENTION_DAYS);
            } else {
                log.warn("[Job] 操作日志清理远程触发失败: {}", resp == null ? "无响应" : resp.getMsg());
            }
        } catch (Exception e) {
            log.error("[Job] 操作日志清理失败", e);
        } finally {
            jobLock.unlock("operLogClean");
        }
    }
}
