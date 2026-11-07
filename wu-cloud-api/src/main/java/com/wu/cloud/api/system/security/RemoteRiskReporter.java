package com.wu.cloud.api.system.security;

import com.wu.cloud.api.system.dto.SecurityEventDTO;
import com.wu.cloud.api.system.feign.RemoteSecurityService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.risk.RiskReporter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 OpenFeign 的风险事件上报实现（各服务通用）。
 * <p>
 * 所有服务共用同一份实现：事件统一交给风控属主 wu-service-system 计分与封禁，
 * 避免各服务各写一套阈值导致的「同一主体在不同服务分数不一致」。
 * </p>
 */
@Slf4j
@Component
public class RemoteRiskReporter implements RiskReporter {

    @Resource
    private RemoteSecurityService remoteSecurityService;

    @Override
    public void report(String eventType, String principal, String ip, int delta, String action,
                       String uri, String method, String userAgent, String reason) {
        try {
            SecurityEventDTO dto = new SecurityEventDTO();
            dto.setEventType(eventType);
            dto.setPrincipal(principal);
            dto.setIp(ip);
            dto.setDelta(delta);
            dto.setAction(action);
            dto.setUri(uri);
            dto.setMethod(method);
            dto.setUserAgent(userAgent);
            dto.setReason(reason);
            remoteSecurityService.reportEvent(dto);
        } catch (Exception e) {
            // 上报属于旁路动作，失败不阻断业务请求
            log.warn("[Risk] 风险事件上报失败 eventType={} principal={} err={}",
                    eventType, principal, e.getMessage());
        }
    }

    @Override
    public boolean isBlocked(String principal) {
        try {
            R<Boolean> resp = remoteSecurityService.isBlocked(principal);
            return resp != null && resp.isSuccess() && Boolean.TRUE.equals(resp.getData());
        } catch (Exception e) {
            log.warn("[Risk] 封禁状态查询失败，放行 principal={} err={}", principal, e.getMessage());
            return false;
        }
    }
}
