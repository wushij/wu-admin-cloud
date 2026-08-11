package com.wu.cloud.infra.framework;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.mapper.AccessLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * API 访问日志采集拦截器（P1 修复：落库）
 * <p>
 * 原实现只打日志不落库。本拦截器在请求完成后写入 {@code infra_access_log} 表，
 * 供监控页面统计与查询（配合 {@code SystemJobTask.purgeExpiredLogs} 定期清理）。
 * </p>
 */
@Slf4j
@Component
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "apiAccessStartTime";

    @Resource
    private AccessLogMapper accessLogMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, @Nullable Exception ex) {
        try {
            String path = request.getRequestURI();
            if (shouldExclude(path)) {
                return;
            }
            Long start = (Long) request.getAttribute(START_TIME_ATTR);
            if (start == null) {
                start = System.currentTimeMillis();
            }
            long duration = System.currentTimeMillis() - start;

            AccessLogDO accessLog = new AccessLogDO();
            accessLog.setTraceId(org.slf4j.MDC.get("traceId"));
            accessLog.setRequestUrl(path);
            accessLog.setRequestMethod(request.getMethod());
            accessLog.setUserAgent(truncate(request.getHeader("User-Agent"), 255));
            accessLog.setRemoteAddr(resolveIp(request));
            accessLog.setApplicationName("wu-service-infra");
            accessLog.setStatus(response.getStatus());
            accessLog.setDuration(duration);
            accessLog.setAccessTime(LocalDateTime.now());
            Long userId = UserContextHolder.getUserId();
            if (userId != null) {
                accessLog.setUserId(String.valueOf(userId));
            }
            String username = UserContextHolder.getUsername();
            if (username != null) {
                accessLog.setUserName(username);
            }
            accessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.warn("[ApiAccessLog] 采集异常: {}", e.getMessage());
        }
    }

    private boolean shouldExclude(String path) {
        if (path == null) {
            return true;
        }
        return path.startsWith("/actuator")
                || path.startsWith("/druid")
                || path.startsWith("/error")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.startsWith("/webjars")
                || path.contains("/access-log")
                || path.contains(".");
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
