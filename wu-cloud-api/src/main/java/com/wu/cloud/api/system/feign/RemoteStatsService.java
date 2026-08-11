package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteStatsServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 远程系统统计服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteStatsService", value = "wu-service-system", path = "/inner/stats", fallbackFactory = RemoteStatsServiceFallback.class)
public interface RemoteStatsService {

    /**
     * 各维度基础数量统计
     */
    @GetMapping("/count")
    R<Map<String, Long>> countAll();

    /**
     * 工作台聚合统计（含今日/昨日新增、登录数与环比趋势）
     */
    @GetMapping("/dashboard")
    R<Map<String, Object>> getDashboardStats();

    /**
     * 最近登录记录
     *
     * @param limit 返回条数
     */
    @GetMapping("/recent-logins")
    R<List<RecentLoginDTO>> getRecentLogins(@RequestParam("limit") Integer limit);

    /**
     * 记录一次工作台访问
     */
    @PostMapping("/visit")
    R<Boolean> recordVisit();
}
