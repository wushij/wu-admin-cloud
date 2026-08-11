package com.wu.cloud.infra.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.infra.entity.SysJobLogDO;
import com.wu.cloud.infra.mapper.SysJobLogMapper;
import com.wu.cloud.infra.service.SysJobLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务执行日志服务实现
 */
@Service
public class SysJobLogServiceImpl implements SysJobLogService {

    @Resource
    private SysJobLogMapper sysJobLogMapper;

    @Override
    public void record(SysJobLogDO log) {
        sysJobLogMapper.insert(log);
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        Long totalCount = sysJobLogMapper.selectCount(new LambdaQueryWrapper<SysJobLogDO>()
                .eq(SysJobLogDO::getDeleted, 0));
        Long successCount = sysJobLogMapper.selectCount(new LambdaQueryWrapper<SysJobLogDO>()
                .eq(SysJobLogDO::getDeleted, 0)
                .eq(SysJobLogDO::getStatus, 0));
        Long failCount = sysJobLogMapper.selectCount(new LambdaQueryWrapper<SysJobLogDO>()
                .eq(SysJobLogDO::getDeleted, 0)
                .eq(SysJobLogDO::getStatus, 1));
        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        long total = totalCount == null ? 0 : totalCount;
        long success = successCount == null ? 0 : successCount;
        result.put("successRate", total == 0 ? 100.0 : Math.round(success * 1000.0 / total) / 10.0);
        result.put("dailyStats", sysJobLogMapper.selectDailyStats());
        return result;
    }

    @Override
    public PageResult<SysJobLogDO> page(Integer pageNo, Integer pageSize,
                                        String jobName, String jobGroup, Integer status) {
        int current = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        List<SysJobLogDO> list = sysJobLogMapper.selectPage(jobName, jobGroup, status,
                (long) (current - 1) * size, size);
        long total = sysJobLogMapper.selectPageCount(jobName, jobGroup, status);
        return PageResult.of(list, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanAll() {
        sysJobLogMapper.update(null, new LambdaUpdateWrapper<SysJobLogDO>()
                .eq(SysJobLogDO::getDeleted, 0)
                .set(SysJobLogDO::getDeleted, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanScope(String jobName, String jobGroup) {
        sysJobLogMapper.update(null, new LambdaUpdateWrapper<SysJobLogDO>()
                .eq(SysJobLogDO::getDeleted, 0)
                .like(jobName != null && !jobName.isBlank(), SysJobLogDO::getJobName, jobName)
                .eq(jobGroup != null && !jobGroup.isBlank(), SysJobLogDO::getJobGroup, jobGroup)
                .set(SysJobLogDO::getDeleted, 1));
    }

    @Override
    public PageResult<SysJobLogDO> recyclePage(Integer pageNo, Integer pageSize,
                                               String jobName, String jobGroup) {
        int current = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        List<SysJobLogDO> list = sysJobLogMapper.selectDeletedPage(jobName, jobGroup,
                (long) (current - 1) * size, size);
        long total = sysJobLogMapper.selectDeletedCount(jobName, jobGroup);
        return PageResult.of(list, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        // 注意：selectById 受 @TableLogic 过滤（只查 deleted=0），
        // 恢复回收站记录必须用自定义 SQL 绕过逻辑删除过滤
        sysJobLogMapper.restoreById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        // 物理删除，绕过 @TableLogic 的逻辑删除
        sysJobLogMapper.physicalDeleteById(id);
    }
}
