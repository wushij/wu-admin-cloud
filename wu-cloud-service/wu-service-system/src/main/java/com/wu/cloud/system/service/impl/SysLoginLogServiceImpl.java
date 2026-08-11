package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.mapper.SysLoginLogMapper;
import com.wu.cloud.system.service.SysLoginLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 登录日志服务实现
 */
@Service
public class SysLoginLogServiceImpl implements SysLoginLogService {

    /** 单页最大条数，防止恶意大分页 */
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    @Override
    public void recordLoginLog(SysLoginLogDO logDO) {
        if (logDO.getLoginTime() == null) {
            logDO.setLoginTime(LocalDateTime.now());
        }
        sysLoginLogMapper.insert(logDO);
    }

    @Override
    public Page<SysLoginLogDO> pageByUsername(String username, Integer pageNo, Integer pageSize) {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);

        Page<SysLoginLogDO> page = new Page<>(no, size);
        LambdaQueryWrapper<SysLoginLogDO> wrapper = new LambdaQueryWrapper<SysLoginLogDO>()
                .eq(SysLoginLogDO::getUsername, username)
                .orderByDesc(SysLoginLogDO::getLoginTime);
        return sysLoginLogMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<SysLoginLogDO> getLoginLogPage(PageQuery pageQuery, String username, String ip, Integer status) {
        int no = pageQuery == null || pageQuery.getPageNo() == null || pageQuery.getPageNo() < 1
                ? 1 : pageQuery.getPageNo();
        int size = pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1
                ? 10 : Math.min(pageQuery.getPageSize(), MAX_PAGE_SIZE);

        Page<SysLoginLogDO> page = new Page<>(no, size);
        LambdaQueryWrapper<SysLoginLogDO> wrapper = new LambdaQueryWrapper<SysLoginLogDO>()
                .like(StringUtils.hasText(username), SysLoginLogDO::getUsername, username)
                .like(StringUtils.hasText(ip), SysLoginLogDO::getIp, ip)
                .eq(status != null, SysLoginLogDO::getStatus, status)
                .orderByDesc(SysLoginLogDO::getLoginTime);
        return sysLoginLogMapper.selectPage(page, wrapper);
    }

    @Override
    public void deleteLoginLog(Long id) {
        // sys_login_log 未启用逻辑删除，此处为物理删除
        sysLoginLogMapper.deleteById(id);
    }

    @Override
    public void clearLoginLog() {
        // 清空全表：传入空条件的 Wrapper，MyBatis-Plus 会生成不带 WHERE 的 DELETE
        sysLoginLogMapper.delete(new LambdaQueryWrapper<>());
    }
}
