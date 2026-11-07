package com.wu.cloud.monitor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * <p>路径说明：前端直接调用 {@code /api/monitor/server|cache/**}（网关路由 {@code Path=/api/monitor/**}），
 * 不依赖任何网关重写规则。</p>
 */
@Tag(name = "服务监控")
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
    @Operation(summary = "获取详情")
    @GetMapping("/server")
    public R<ServerInfoVO> getServerInfo() {
        return R.ok(serverMonitorService.getServerInfo());
    }

    /** B 队修复：GET /api/monitor/server/info（前端 getServerInfo 调用） */
    @RequiresPermissions("monitor:server:list")
    @Operation(summary = "Redis 服务信息")
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
    @Operation(summary = "获取详情")
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
    @Operation(summary = "Redis 服务信息")
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
    @Operation(summary = "Redis 实时统计（图表）")
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
    @Operation(summary = "扫描缓存键（SCAN，上限 500）")
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
    @Operation(summary = "查看缓存键详情")
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
    @Operation(summary = "删除")
    @DeleteMapping("/cache")
    public R<Boolean> deleteCacheKey(@RequestParam("key") String key) {
        CacheMonitorService service = cache();
        if (service == null) {
            return R.ok(false);
        }
        return R.ok(service.deleteKey(key));
    }
}
