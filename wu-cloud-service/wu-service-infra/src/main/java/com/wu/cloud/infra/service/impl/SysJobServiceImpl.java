package com.wu.cloud.infra.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.SysJobDO;
import com.wu.cloud.infra.entity.SysJobLogDO;
import com.wu.cloud.infra.job.DistributedJobLock;
import com.wu.cloud.infra.mapper.SysJobMapper;
import com.wu.cloud.infra.service.SysJobLogService;
import com.wu.cloud.infra.service.SysJobService;
import com.wu.cloud.infra.vo.JobTemplateVO;
import com.wu.cloud.infra.vo.SysJobVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务服务实现（DB 持久化 + Spring TaskScheduler 调度 + 执行日志 + 回收站）。
 * <p>C 队修复：将原内存态 JobController 升级为基于 sys_job/sys_job_log 表的完整实现，
 * 启动时从 DB 加载任务，动态增删改即生效，多实例由 DistributedJobLock 保证不重复执行。</p>
 */
@Slf4j
@Service
public class SysJobServiceImpl implements SysJobService {

    /** 任务运行态：jobId -> ScheduledFuture */
    private final Map<Long, ScheduledFuture<?>> runningFutures = new ConcurrentHashMap<>();

    @Resource
    private SysJobMapper sysJobMapper;
    @Resource
    private SysJobLogService sysJobLogService;
    @Resource
    private TaskScheduler taskScheduler;
    @Resource
    private DistributedJobLock distributedJobLock;
    @Resource
    private ApplicationContext applicationContext;

    @PostConstruct
    @Override
    public void initScheduler() {
        List<SysJobDO> jobs = sysJobMapper.selectList(
                new LambdaQueryWrapper<SysJobDO>().eq(SysJobDO::getStatus, 1));
        for (SysJobDO job : jobs) {
            schedule(job);
        }
        log.info("[Job] 启动加载 {} 个正常任务到调度器", jobs.size());
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>(sysJobLogService.statistics());
        Long totalJobs = sysJobMapper.selectCount(new LambdaQueryWrapper<SysJobDO>()
                .eq(SysJobDO::getDeleted, 0));
        Long runningJobs = sysJobMapper.selectCount(new LambdaQueryWrapper<SysJobDO>()
                .eq(SysJobDO::getDeleted, 0)
                .eq(SysJobDO::getStatus, 1));
        result.put("totalJobs", totalJobs);
        result.put("runningJobs", runningJobs);
        result.put("pausedJobs", Math.max(0, (totalJobs == null ? 0 : totalJobs) - (runningJobs == null ? 0 : runningJobs)));
        return result;
    }

    @Override
    public List<JobTemplateVO> templates() {
        List<JobTemplateVO> list = new ArrayList<>();
        list.add(template("purge_logs", "过期日志归档", "SYSTEM",
                "清理超保留期的操作/登录/API 访问日志", "systemJobTask.purgeExpiredLogs",
                "0 30 2 * * ?", "系统日志", "Document"));
        list.add(template("purge_job_log", "调度日志清理", "SYSTEM",
                "清理 30 天前的 Quartz 调度执行日志", "systemJobTask.purgeExpiredJobLogs",
                "0 0 4 ? * SUN", "定时任务", "Timer"));
        list.add(template("purge_chat", "私聊消息清理", "SYSTEM",
                "清理超过 180 天的私聊记录", "systemJobTask.purgeOldChatMessages",
                "0 0 3 * * ?", "消息聊天", "ChatDotRound"));
        list.add(template("purge_notices", "已读通知清理", "SYSTEM",
                "清理已读且超过 90 天的站内通知", "systemJobTask.purgeReadNotices",
                "0 15 3 * * ?", "消息中心", "Bell"));
        return list;
    }

    private JobTemplateVO template(String key, String name, String group, String desc,
                                   String target, String cron, String module, String icon) {
        JobTemplateVO vo = new JobTemplateVO();
        vo.setKey(key);
        vo.setName(name);
        vo.setCategory("SYSTEM");
        vo.setJobGroup(group);
        vo.setDescription(desc);
        vo.setInvokeTarget(target);
        vo.setCronExpression(cron);
        vo.setRelatedModule(module);
        vo.setIcon(icon);
        return vo;
    }

    @Override
    public Map<String, Object> checkCron(String cronExpression) {
        Map<String, Object> result = new HashMap<>();
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            result.put("valid", true);
            result.put("hint", "Cron 表达式有效");
            List<String> nexts = new ArrayList<>();
            ZonedDateTime now = ZonedDateTime.now();
            for (int i = 0; i < 5; i++) {
                now = cron.next(now);
                if (now == null) {
                    break;
                }
                nexts.add(now.toLocalDateTime().toString().replace("T", " "));
            }
            result.put("nextFireTimes", nexts);
        } catch (Exception e) {
            result.put("valid", false);
            result.put("hint", "Cron 表达式无效：" + e.getMessage());
            result.put("nextFireTimes", new ArrayList<>());
        }
        return result;
    }

    @Override
    public PageResult<SysJobVO> pageVo(Integer pageNo, Integer pageSize,
                                       String jobName, String jobGroup, Integer status) {
        int current = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        List<SysJobDO> list = sysJobMapper.selectPage(jobName, jobGroup, status,
                (long) (current - 1) * size, size);
        long total = sysJobMapper.selectPageCount(jobName, jobGroup, status);
        List<SysJobVO> voList = new ArrayList<>();
        for (SysJobDO job : list) {
            SysJobVO vo = new SysJobVO();
            BeanUtils.copyProperties(job, vo);
            vo.setCronHint(cronHint(job.getCronExpression()));
            vo.setNextFireTime(nextFireTime(job));
            voList.add(vo);
        }
        return PageResult.of(voList, total);
    }

    @Override
    public SysJobDO getById(Long id) {
        return sysJobMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysJobDO job) {
        if (!StringUtils.hasText(job.getJobGroup())) {
            job.setJobGroup("DEFAULT");
        }
        if (job.getMisfirePolicy() == null) {
            job.setMisfirePolicy(3);
        }
        if (job.getConcurrent() == null) {
            job.setConcurrent(1);
        }
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        validateCron(job.getCronExpression());
        sysJobMapper.insert(job);
        if (job.getStatus() == 1) {
            schedule(job);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysJobDO job) {
        SysJobDO exist = sysJobMapper.selectById(job.getId());
        if (exist == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        validateCron(job.getCronExpression());
        // 先取消旧调度
        cancel(job.getId());
        sysJobMapper.updateById(job);
        if (job.getStatus() == 1) {
            schedule(sysJobMapper.selectById(job.getId()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysJobDO job = sysJobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        cancel(id);
        sysJobMapper.deleteById(id); // 逻辑删除（@TableLogic → deleted=1）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        SysJobDO job = sysJobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        job.setStatus(status);
        sysJobMapper.updateById(job);
        if (status != null && status == 1) {
            schedule(sysJobMapper.selectById(id));
        } else {
            cancel(id);
        }
    }

    @Override
    public void run(Long id) {
        SysJobDO job = sysJobMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        executeJob(job);
    }

    @Override
    public PageResult<SysJobDO> recyclePage(Integer pageNo, Integer pageSize,
                                            String jobName, String jobGroup) {
        int current = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        List<SysJobDO> list = sysJobMapper.selectDeletedPage(jobName, jobGroup,
                (long) (current - 1) * size, size);
        long total = sysJobMapper.selectDeletedCount(jobName, jobGroup);
        return PageResult.of(list, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        // 注意：selectById 受 @TableLogic 过滤（只查 deleted=0），
        // 恢复回收站记录必须用自定义 SQL 绕过逻辑删除过滤
        sysJobMapper.restoreById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        // 物理删除，绕过 @TableLogic 的逻辑删除
        sysJobMapper.physicalDeleteById(id);
    }

    // ===== 私有调度逻辑 =====

    private void schedule(SysJobDO job) {
        cancel(job.getId());
        if (!StringUtils.hasText(job.getCronExpression())) {
            return;
        }
        try {
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeJob(job), new CronTrigger(job.getCronExpression()));
            runningFutures.put(job.getId(), future);
        } catch (Exception e) {
            log.warn("[Job] 调度任务失败 id={} cron={}", job.getId(), job.getCronExpression(), e);
        }
    }

    private void cancel(Long jobId) {
        ScheduledFuture<?> future = runningFutures.remove(jobId);
        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    private void executeJob(SysJobDO job) {
        boolean locked = false;
        try {
            // 禁止并发时用分布式锁保证多实例不重复执行
            if (job.getConcurrent() != null && job.getConcurrent() == 1) {
                if (!distributedJobLock.tryLock("job:" + job.getId())) {
                    log.debug("[Job] 已被其他实例执行，跳过 id={}", job.getId());
                    return;
                }
                locked = true;
            }
            LocalDateTime start = LocalDateTime.now();
            invokeTarget(job.getInvokeTarget());
            LocalDateTime stop = LocalDateTime.now();
            recordLog(job, null, start, stop);
        } catch (Throwable e) {
            LocalDateTime stop = LocalDateTime.now();
            recordLog(job, e, stop.minusSeconds(1), stop);
            log.error("[Job] 执行失败 id={} target={}", job.getId(), job.getInvokeTarget(), e);
        } finally {
            if (locked) {
                distributedJobLock.unlock("job:" + job.getId());
            }
        }
    }

    /**
     * 反射调用 invokeTarget（格式：beanName.methodName）
     */
    private void invokeTarget(String invokeTarget) throws Exception {
        if (!StringUtils.hasText(invokeTarget) || !invokeTarget.contains(".")) {
            log.warn("[Job] invokeTarget 格式不合法：{}", invokeTarget);
            return;
        }
        int idx = invokeTarget.lastIndexOf('.');
        String beanName = invokeTarget.substring(0, idx);
        String methodName = invokeTarget.substring(idx + 1);
        Object bean = applicationContext.getBean(beanName);
        Method method = bean.getClass().getMethod(methodName);
        method.invoke(bean);
    }

    private void recordLog(SysJobDO job, Throwable error, LocalDateTime start, LocalDateTime stop) {
        SysJobLogDO logEntry = new SysJobLogDO();
        logEntry.setJobName(job.getJobName());
        logEntry.setJobGroup(job.getJobGroup());
        logEntry.setInvokeTarget(job.getInvokeTarget());
        logEntry.setStartTime(start);
        logEntry.setStopTime(stop);
        logEntry.setDurationMs(java.time.Duration.between(start, stop).toMillis());
        if (error == null) {
            logEntry.setStatus(0);
            logEntry.setJobMessage("执行成功");
        } else {
            logEntry.setStatus(1);
            logEntry.setJobMessage("执行失败");
            logEntry.setExceptionInfo(String.valueOf(error).length() > 2000
                    ? String.valueOf(error).substring(0, 2000) : String.valueOf(error));
        }
        try {
            sysJobLogService.record(logEntry);
        } catch (Exception e) {
            log.error("[Job] 记录日志失败", e);
        }
    }

    private void validateCron(String cron) {
        if (!StringUtils.hasText(cron)) {
            throw new IllegalArgumentException("cron 表达式不能为空");
        }
        try {
            CronExpression.parse(cron);
        } catch (Exception e) {
            throw new IllegalArgumentException("cron 表达式无效：" + cron, e);
        }
    }

    private String cronHint(String cron) {
        if (!StringUtils.hasText(cron)) {
            return "";
        }
        try {
            CronExpression.parse(cron);
            return "Cron 表达式";
        } catch (Exception e) {
            return "Cron 无效";
        }
    }

    private String nextFireTime(SysJobDO job) {
        if (job.getStatus() == null || job.getStatus() != 1) {
            return null;
        }
        try {
            CronExpression cron = CronExpression.parse(job.getCronExpression());
            ZonedDateTime next = cron.next(ZonedDateTime.now());
            return next == null ? null : next.toLocalDateTime().toString().replace("T", " ");
        } catch (Exception e) {
            return null;
        }
    }
}
