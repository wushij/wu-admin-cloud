package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.entity.OperLogDO;
import com.wu.cloud.infra.entity.SysJobLogDO;
import com.wu.cloud.infra.mapper.AccessLogMapper;
import com.wu.cloud.infra.mapper.OperLogMapper;
import com.wu.cloud.infra.mapper.SysJobLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时任务 Bean（sys_job 初始数据 invokeTarget 引用的目标）
 * <p>
 * 微服务拆分后，工单/消息/通知等数据分属其它服务库，本服务仅实现 infra 域内的清理任务；
 * 跨服务的清理（私聊消息、群聊消息、已读通知、工单回收站）在微服务架构下由对应服务
 * 各自调度，此处保留方法签名避免反射失败，并记录提示日志。
 * </p>
 */
@Slf4j
@Component("systemJobTask")
public class SystemJobTask {

    @Resource
    private AccessLogMapper accessLogMapper;
    @Resource
    private OperLogMapper operLogMapper;
    @Resource
    private SysJobLogMapper sysJobLogMapper;

    /** 清理超保留期的 API 访问日志（默认 90 天） */
    public void purgeExpiredLogs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(90);
        int deleted = accessLogMapper.delete(
                new LambdaQueryWrapper<AccessLogDO>().lt(AccessLogDO::getAccessTime, deadline));
        log.info("[Job] 访问日志清理完成，删除 {} 条（90 天前）", deleted);
    }

    /** 清理超保留期的操作日志（默认 180 天） */
    public void purgeExpiredOperLogs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(180);
        int deleted = operLogMapper.delete(
                new LambdaQueryWrapper<OperLogDO>().lt(OperLogDO::getOperTime, deadline));
        log.info("[Job] 操作日志清理完成，删除 {} 条（180 天前）", deleted);
    }

    /** 清理超保留期的调度执行日志（默认 30 天） */
    public void purgeExpiredJobLogs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(30);
        int deleted = sysJobLogMapper.delete(
                new LambdaQueryWrapper<SysJobLogDO>().lt(SysJobLogDO::getStartTime, deadline));
        log.info("[Job] 调度日志清理完成，删除 {} 条（30 天前）", deleted);
    }

    /**
     * 私聊消息清理：微服务架构下由消息服务调度，本服务无对应库，仅记录提示
     */
    public void purgeOldChatMessages() {
        log.info("[Job] 私聊消息清理已移交消息服务调度，本服务跳过");
    }

    /**
     * 群聊消息清理：微服务架构下由消息服务调度，本服务无对应库，仅记录提示
     */
    public void purgeOldGroupChatMessages() {
        log.info("[Job] 群聊消息清理已移交消息服务调度，本服务跳过");
    }

    /**
     * 已读通知清理：微服务架构下由消息服务调度，本服务无对应库，仅记录提示
     */
    public void purgeReadNotices() {
        log.info("[Job] 已读通知清理已移交消息服务调度，本服务跳过");
    }

    /**
     * 工单回收站清理：微服务架构下由工单服务调度，本服务无对应库，仅记录提示
     */
    public void purgeTicketRecycleBin() {
        log.info("[Job] 工单回收站清理已移交工单服务调度，本服务跳过");
    }
}
