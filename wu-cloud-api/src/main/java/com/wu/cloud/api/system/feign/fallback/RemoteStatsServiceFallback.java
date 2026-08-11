package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.api.system.feign.RemoteStatsService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 远程系统统计服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteStatsServiceFallback implements FallbackFactory<RemoteStatsService> {

    @Override
    public RemoteStatsService create(Throwable cause) {
        log.error("[Feign降级] wu-service-system 统计服务调用失败", cause);
        return new RemoteStatsService() {
            @Override
            public R<Map<String, Long>> countAll() {
                return R.fail("统计服务暂不可用");
            }

            @Override
            public R<Map<String, Object>> getDashboardStats() {
                return R.fail("统计服务暂不可用");
            }

            @Override
            public R<List<RecentLoginDTO>> getRecentLogins(Integer limit) {
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<Boolean> recordVisit() {
                return R.fail("统计服务暂不可用");
            }
        };
    }
}
