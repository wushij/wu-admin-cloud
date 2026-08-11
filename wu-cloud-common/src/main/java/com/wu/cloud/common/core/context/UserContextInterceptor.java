package com.wu.cloud.common.core.context;

import com.wu.cloud.common.core.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 全局 Web 请求上下文与 TraceId 解析拦截器
 * <p>
 * 从网关透传的 {@code X-User-Id} / {@code X-User-Name} / {@code X-User-Roles} /
 * {@code X-User-Permissions} 头还原 {@link UserContextHolder}，并把 {@code X-Trace-Id}
 * 写入 MDC 以便日志串联。请求结束后清理 ThreadLocal 与 MDC，避免线程复用串号。
 * </p>
 * <p>
 * 网关未下发用户头时（白名单/匿名接口）上下文为空，由业务自行处理未登录态。
 * </p>
 */
public class UserContextInterceptor implements HandlerInterceptor {

    /** MDC 中链路追踪 ID 的键名 */
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(HeaderConstants.TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            MDC.put(MDC_TRACE_ID, traceId);
        }

        String userIdStr = request.getHeader(HeaderConstants.USER_ID);
        if (userIdStr == null || userIdStr.isBlank()) {
            return true;
        }

        try {
            UserContext context = new UserContext();
            context.setUserId(Long.parseLong(userIdStr.trim()));
            context.setUsername(decode(request.getHeader(HeaderConstants.USER_NAME)));
            context.setRoles(splitToSet(request.getHeader(HeaderConstants.USER_ROLES)));
            context.setPermissions(splitToSet(request.getHeader(HeaderConstants.USER_PERMISSIONS)));
            UserContextHolder.set(context);
        } catch (NumberFormatException ignored) {
            // 用户头非法时视为匿名访问，交由权限切面拦截
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContextHolder.remove();
        MDC.remove(MDC_TRACE_ID);
    }

    /**
     * 将逗号分隔的头值切分为集合，保持顺序且忽略空白项
     */
    private Set<String> splitToSet(String raw) {
        String value = decode(raw);
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * HTTP 头只允许 ASCII，网关对中文等非 ASCII 内容做了 URL 编码，此处还原
     */
    private String decode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return raw;
        }
    }
}
