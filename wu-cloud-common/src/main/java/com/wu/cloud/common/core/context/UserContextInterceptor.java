package com.wu.cloud.common.core.context;

import com.wu.cloud.common.core.constant.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 内网零信任：配置了 {@code wu.security.inner-token} 后，{@code /inner/**} 端点
 * 强制校验请求头 {@code X-Inner-Token}（由 FeignAuthInterceptor 为服务间调用自动携带），
 * 未携带或不匹配直接拒绝（403），防止内网直连绕过网关调用高危内部接口。
 * </p>
 * <p>
 * 网关未下发用户头时（白名单/匿名接口）上下文为空，由业务自行处理未登录态。
 * </p>
 */
public class UserContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(UserContextInterceptor.class);

    /** MDC 中链路追踪 ID 的键名 */
    public static final String MDC_TRACE_ID = "traceId";

    /** 内网端点路径前缀 */
    private static final String INNER_API_PREFIX = "/inner/";

    /** 内网令牌；为空表示未启用零信任校验（仅限开发环境） */
    private final String innerToken;

    public UserContextInterceptor() {
        this("");
    }

    public UserContextInterceptor(String innerToken) {
        this.innerToken = innerToken == null ? "" : innerToken.trim();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        // 内网端点零信任校验：令牌已配置时强制匹配
        if (!innerToken.isEmpty() && request.getRequestURI() != null
                && request.getRequestURI().startsWith(INNER_API_PREFIX)) {
            String presented = request.getHeader(HeaderConstants.INNER_TOKEN);
            if (presented == null || !innerToken.equals(presented.trim())) {
                log.warn("[InnerApi] 内网端点令牌校验失败 uri={} ip={}",
                        request.getRequestURI(), request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"msg\":\"forbidden\"}");
                return false;
            }
        }

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
