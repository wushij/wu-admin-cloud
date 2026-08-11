package com.wu.cloud.monitor.service;

import com.wu.cloud.monitor.vo.CacheInfoVO;
import com.wu.cloud.monitor.vo.CacheKeysVO;
import com.wu.cloud.monitor.vo.CacheStatsVO;
import com.wu.cloud.monitor.vo.CacheValueVO;

/**
 * Redis 缓存监控服务
 */
public interface CacheMonitorService {

    /**
     * 获取 Redis 运行指标、内存占用与命令统计
     *
     * @return 缓存监控快照，Redis 不可达时 available=false
     */
    CacheInfoVO getCacheInfo();

    /**
     * 获取 Redis 统计信息（内存、命中率、连接数等）
     */
    CacheStatsVO getStats();

    /**
     * 按 pattern 扫描 Redis key（限制条数，避免阻塞）
     *
     * @param pattern 匹配模式，如 * / user:* / satoken:*
     * @param limit   最多返回条数
     */
    CacheKeysVO scanKeys(String pattern, long limit);

    /**
     * 获取指定 key 的 value 详情（类型、TTL、值）
     */
    CacheValueVO getValue(String key);

    /**
     * 删除指定 key
     */
    boolean deleteKey(String key);
}
