package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.message.feign.RemoteMessageService;
import com.wu.cloud.api.system.feign.RemoteLogService;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.entity.SysJobLogDO;
import com.wu.cloud.infra.mapper.AccessLogMapper;
import com.wu.cloud.infra.mapper.SysJobLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务 Bean（sys_job 初始数据 invokeTarget 引用的目标）
 * <p>
 * 调度归口本服务：infra 域内日志本地清理；操作日志/工单/消息域数据
 * 由属主服务提供内部清理端点，本任务统一通过 OpenFeign 远程触发并记录清理条数。
 * </p>
 */
@Slf4j
@Component("systemJobTask")
public class SystemJobTask {

    @Resource
    private AccessLogMapper accessLogMapper;
    @Resource
    private SysJobLogMapper sysJobLogMapper;
    @Resource
    private RemoteLogService remoteLogService;
    @Resource
    private RemoteMessageService remoteMessageService;
    @Resource
    private RemoteTicketService remoteTicketService;

    /** 清理超保留期的 API 访问日志（默认 90 天） */
    public void purgeExpiredLogs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(90);
        int deleted = accessLogMapper.delete(
                new LambdaQueryWrapper<AccessLogDO>().lt(AccessLogDO::getAccessTime, deadline));
        log.info("[Job] 访问日志清理完成，删除 {} 条（90 天前）", deleted);
    }

    /**
     * 清理超保留期的操作日志（默认 180 天）。
     * 操作日志属主为 wu-service-system，远程触发其在本地库清理 sys_oper_log。
     */
    public void purgeExpiredOperLogs() {
        R<Integer> resp = remoteLogService.purgeExpiredOperLogs(180);
        if (resp != null && resp.isSuccess()) {
            log.info("[Job] 操作日志清理完成，删除 {} 条（180 天前，由 wu-service-system 执行）", resp.getData());
        } else {
            log.warn("[Job] 操作日志清理远程触发失败: {}", resp == null ? "无响应" : resp.getMsg());
        }
    }

    /** 清理超保留期的调度执行日志（默认 30 天） */
    public void purgeExpiredJobLogs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);
        int deleted = sysJobLogMapper.delete(
                new LambdaQueryWrapper<SysJobLogDO>().lt(SysJobLogDO::getStartTime, deadline));
        log.info("[Job] 调度日志清理完成，删除 {} 条（30 天前）", deleted);
    }

    /**
     * 私聊消息清理：远程触发 wu-service-message 在其本地库执行
     */
    public void purgeOldChatMessages() {
        R<Integer> resp = remoteMessageService.purgeOldChatMessages(180);
        log.info("[Job] 私聊消息清理{}，删除 {} 条",
                resp != null && resp.isSuccess() ? "完成" : "远程触发失败", resp == null ? 0 : resp.getData());
    }

    /**
     * 群聊消息清理：远程触发 wu-service-message 在其本地库执行
     */
    public void purgeOldGroupChatMessages() {
        R<Integer> resp = remoteMessageService.purgeOldGroupChatMessages(180);
        log.info("[Job] 群聊消息清理{}，删除 {} 条",
                resp != null && resp.isSuccess() ? "完成" : "远程触发失败", resp == null ? 0 : resp.getData());
    }

    /**
     * 已读通知清理：远程触发 wu-service-message 在其本地库执行
     */
    public void purgeReadNotices() {
        R<Integer> resp = remoteMessageService.purgeReadNotices(180);
        log.info("[Job] 已读通知清理{}，删除 {} 条",
                resp != null && resp.isSuccess() ? "完成" : "远程触发失败", resp == null ? 0 : resp.getData());
    }

    /**
     * 工单回收站清理：远程触发 wu-service-ticket 在其本地库执行
     */
    public void purgeTicketRecycleBin() {
        R<Integer> resp = remoteTicketService.purgeRecycleBin(30);
        log.info("[Job] 工单回收站清理{}，删除 {} 条",
                resp != null && resp.isSuccess() ? "完成" : "远程触发失败", resp == null ? 0 : resp.getData());
    }
}
