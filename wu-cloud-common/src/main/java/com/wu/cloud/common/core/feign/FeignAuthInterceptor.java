package com.wu.cloud.common.core.feign;

import com.wu.cloud.common.core.constant.HeaderConstants;
import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.context.UserContextInterceptor;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * Feign 服务间 RPC 上下文与 TraceId 自动透传拦截器 (底层基础设施级通信组件)
 * <p>
 * 将当前线程的用户身份、角色与权限透传给下游服务，使下游的
 * {@code @RequiresPermissions} 切面在 RPC 链路上依然可以正确判权。
 * </p>
 */
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        UserContext user = UserContextHolder.get();
        if (user != null) {
            if (user.getUserId() != null) {
                template.header(HeaderConstants.USER_ID, user.getUserId().toString());
            }
            putIfPresent(template, HeaderConstants.USER_NAME, user.getUsername());
            putIfPresent(template, HeaderConstants.USER_ROLES, join(user.getRoles()));
            putIfPresent(template, HeaderConstants.USER_PERMISSIONS, join(user.getPermissions()));
        }
        String traceId = MDC.get(UserContextInterceptor.MDC_TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            template.header(HeaderConstants.TRACE_ID, traceId);
        }
    }

    /**
     * 写入请求头，HTTP 头仅允许 ASCII，非 ASCII 内容做 URL 编码，下游统一解码
     */
    private void putIfPresent(RequestTemplate template, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        template.header(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private String join(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }
}
