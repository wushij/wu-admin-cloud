package com.wu.cloud.system.service.cache;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.mapper.SysConfigGroupMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置共享缓存维护组件（配置属主侧）。
 * <p>
 * sys_config_group 的唯一属主是 wu-service-system。本组件在启动与配置变更时，
 * 将全量配置组写入 Redis Hash {@code sys:config:groups}（field=groupCode，value=configValue JSON），
 * 供其他微服务的动态配置加载链（Redis 优先、Feign 兜底）读取，实现配置的跨服务分发。
 * </p>
 */
@Slf4j
@Service
public class SysConfigCacheService {

    public static final String REDIS_KEY = "sys:config:groups";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SysConfigGroupMapper sysConfigGroupMapper;

    /**
     * 全量刷新：以数据库为基准重建共享缓存 Hash
     */
    public void refreshAll() {
        try {
            Map<String, String> snapshot = new HashMap<>();
            for (SysConfigGroupDO group : sysConfigGroupMapper.selectList(null)) {
                if (group.getGroupCode() != null) {
                    snapshot.put(group.getGroupCode(), StrUtil.nullToEmpty(group.getConfigValue()));
                }
            }
            if (snapshot.isEmpty()) {
                return;
            }
            stringRedisTemplate.delete(REDIS_KEY);
            stringRedisTemplate.opsForHash().putAll(REDIS_KEY, snapshot);
            log.info("[SysConfigCache] 共享配置缓存全量刷新完成，共 {} 组", snapshot.size());
        } catch (Exception e) {
            log.warn("[SysConfigCache] 共享配置缓存全量刷新失败（各服务将走 Feign 兜底）: {}", e.getMessage());
        }
    }

    /**
     * 单组刷新：配置更新后立即同步对应 Hash field
     */
    public void refresh(String groupCode) {
        if (StrUtil.isBlank(groupCode)) {
            return;
        }
        try {
            SysConfigGroupDO group = sysConfigGroupMapper.selectOne(new LambdaQueryWrapper<SysConfigGroupDO>()
                    .eq(SysConfigGroupDO::getGroupCode, groupCode)
                    .last("LIMIT 1"));
            if (group == null) {
                stringRedisTemplate.opsForHash().delete(REDIS_KEY, groupCode);
                return;
            }
            stringRedisTemplate.opsForHash().put(REDIS_KEY, groupCode, StrUtil.nullToEmpty(group.getConfigValue()));
        } catch (Exception e) {
            log.warn("[SysConfigCache] 配置组 {} 缓存刷新失败: {}", groupCode, e.getMessage());
        }
    }
}
