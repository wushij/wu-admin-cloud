package com.wu.cloud.monitor.controller;

import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.monitor.service.CacheMonitorService;
import com.wu.cloud.monitor.service.ServerMonitorService;
import com.wu.cloud.monitor.vo.CacheInfoVO;
import com.wu.cloud.monitor.vo.CacheKeysVO;
import com.wu.cloud.monitor.vo.CacheStatsVO;
import com.wu.cloud.monitor.vo.CacheValueVO;
import com.wu.cloud.monitor.vo.ServerInfoVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维监控 API：服务器硬件指标与 Redis 缓存指标
 *
 * <p>B 队修复：网关 compat-monitor-server/cache 将前端 /system/monitor/server|cache/** 重写为
 * /api/monitor/server|cache/**，此处补齐前端实际调用的子路径（/server/info、/cache/stats 等）。</p>
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final ServerMonitorService serverMonitorService;

    /** Redis 为可选依赖，服务未接入 Redis 时该 Bean 不存在 */
    private final ObjectProvider<CacheMonitorService> cacheMonitorServiceProvider;

    public MonitorController(ServerMonitorService serverMonitorService,
                             ObjectProvider<CacheMonitorService> cacheMonitorServiceProvider) {
        this.serverMonitorService = serverMonitorService;
        this.cacheMonitorServiceProvider = cacheMonitorServiceProvider;
    }

    // ===== 服务器监控 =====

    /** 兼容旧端点：GET /api/monitor/server */
    @RequiresPermissions("monitor:server:list")
    @GetMapping("/server")
    public R<ServerInfoVO> getServerInfo() {
        return R.ok(serverMonitorService.getServerInfo());
    }

    /** B 队修复：GET /api/monitor/server/info（前端 getServerInfo 调用） */
    @RequiresPermissions("monitor:server:list")
    @GetMapping("/server/info")
    public R<ServerInfoVO> getServerInfoDetail() {
        return R.ok(serverMonitorService.getServerInfo());
    }

    // ===== 缓存监控 =====

    private CacheMonitorService cache() {
        return cacheMonitorServiceProvider.getIfAvailable();
    }

    /** 兼容旧端点：GET /api/monitor/cache（返回 CacheInfoVO） */
    @RequiresPermissions("monitor:cache:list")
    @GetMapping("/cache")
    public R<CacheInfoVO> getCacheInfo() {
        CacheMonitorService service = cache();
        if (service == null) {
            CacheInfoVO vo = new CacheInfoVO();
            vo.setAvailable(false);
            vo.setMessage("当前服务未接入 Redis，缓存监控不可用");
            return R.ok(vo);
        }
        return R.ok(service.getCacheInfo());
    }

    /** B 队修复：GET /api/monitor/cache/info（前端 getCacheInfo 调用） */
    @RequiresPermissions("monitor:cache:list")
    @GetMapping("/cache/info")
    public R<CacheInfoVO> getCacheInfoDetail() {
        CacheMonitorService service = cache();
        if (service == null) {
            CacheInfoVO vo = new CacheInfoVO();
            vo.setAvailable(false);
            vo.setMessage("当前服务未接入 Redis，缓存监控不可用");
            return R.ok(vo);
        }
        return R.ok(service.getCacheInfo());
    }

    /** B 队修复：GET /api/monitor/cache/stats（前端 getCacheStats 调用） */
    @RequiresPermissions("monitor:cache:list")
    @GetMapping("/cache/stats")
    public R<CacheStatsVO> getCacheStats() {
        CacheMonitorService service = cache();
        if (service == null) {
            return R.ok(new CacheStatsVO());
        }
        return R.ok(service.getStats());
    }

    /** B 队修复：GET /api/monitor/cache/keys（前端 scanCacheKeys 调用） */
    @RequiresPermissions("monitor:cache:list")
    @GetMapping("/cache/keys")
    public R<CacheKeysVO> scanCacheKeys(@RequestParam(value = "pattern", required = false, defaultValue = "*") String pattern,
                                        @RequestParam(value = "limit", required = false, defaultValue = "200") long limit) {
        CacheMonitorService service = cache();
        if (service == null) {
            return R.ok(new CacheKeysVO());
        }
        return R.ok(service.scanKeys(pattern, limit));
    }

    /** B 队修复：GET /api/monitor/cache/value（前端 getCacheValue 调用） */
    @RequiresPermissions("monitor:cache:list")
    @GetMapping("/cache/value")
    public R<CacheValueVO> getCacheValue(@RequestParam("key") String key) {
        CacheMonitorService service = cache();
        if (service == null) {
            return R.ok(new CacheValueVO());
        }
        return R.ok(service.getValue(key));
    }

    /** B 队修复：DELETE /api/monitor/cache（前端 deleteCacheKey 调用） */
    @RequiresPermissions("monitor:cache:delete")
    @DeleteMapping("/cache")
    public R<Boolean> deleteCacheKey(@RequestParam("key") String key) {
        CacheMonitorService service = cache();
        if (service == null) {
            return R.ok(false);
        }
        return R.ok(service.deleteKey(key));
    }
}
