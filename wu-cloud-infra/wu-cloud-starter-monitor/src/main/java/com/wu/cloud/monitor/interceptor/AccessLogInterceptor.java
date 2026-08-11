package com.wu.cloud.monitor.interceptor;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.utils.web.ClientIpUtils;
import com.wu.cloud.monitor.config.MonitorProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * 请求访问日志拦截器：记录请求方法、路径、参数、耗时、状态码与调用者信息。
 *
 * <p>同时向 MDC 写入 traceId，便于全链路日志检索；慢请求以 WARN 级别输出。</p>
 */
@Slf4j
public class AccessLogInterceptor implements HandlerInterceptor {

    /** 请求开始时间在 request 中的属性名 */
    private static final String ATTR_START_TIME = "wu.access.startTime";

    /** MDC 中的链路追踪 ID */
    public static final String MDC_TRACE_ID = "traceId";

    /** 上游网关透传的链路 ID 请求头 */
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    private final MonitorProperties.AccessLog config;

    /** 预先转小写，避免每次请求都做大小写转换 */
    private final Set<String> sensitiveParams;

    public AccessLogInterceptor(MonitorProperties.AccessLog config) {
        this.config = config;
        this.sensitiveParams = new HashSet<>();
        if (config.getSensitiveParams() != null) {
            config.getSensitiveParams().forEach(p -> sensitiveParams.add(p.toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());

        // 复用上游 traceId，缺失时新生成，保证跨服务链路可串联
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(HEADER_TRACE_ID, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Object startAttr = request.getAttribute(ATTR_START_TIME);
            long cost = startAttr instanceof Long start ? System.currentTimeMillis() - start : -1L;

            String uri = request.getRequestURI();
            String method = request.getMethod();
            String ip = ClientIpUtils.resolve(request);
            Long userId = UserContextHolder.getUserId();
            String username = UserContextHolder.getUsername();
            int status = response.getStatus();
            String params = config.isLogRequestParams() ? buildParams(request) : "";

            if (ex != null) {
                log.error("[访问日志] {} {} status={} cost={}ms ip={} user={}({}) params={} 异常={}",
                        method, uri, status, cost, ip, username, userId, params, ex.getMessage(), ex);
            } else if (cost >= 0 && cost >= config.getSlowThresholdMillis()) {
                log.warn("[慢请求] {} {} status={} cost={}ms ip={} user={}({}) params={}",
                        method, uri, status, cost, ip, username, userId, params);
            } else {
                log.info("[访问日志] {} {} status={} cost={}ms ip={} user={}({}) params={}",
                        method, uri, status, cost, ip, username, userId, params);
            }
        } catch (Exception e) {
            // 日志记录本身不得影响业务响应
            log.debug("[访问日志] 记录失败", e);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    /**
     * 拼接查询参数，对敏感字段脱敏并限制总长度
     */
    private String buildParams(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String value = sensitiveParams.contains(key.toLowerCase(Locale.ROOT))
                    ? "******"
                    : String.join(",", entry.getValue());
            joiner.add(key + "=" + value);
        }
        String result = joiner.toString();
        int max = config.getMaxParamLength();
        return (max > 0 && result.length() > max) ? result.substring(0, max) + "...(truncated)" : result;
    }
}
