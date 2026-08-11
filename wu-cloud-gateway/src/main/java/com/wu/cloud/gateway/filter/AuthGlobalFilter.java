package com.wu.cloud.gateway.filter;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.common.core.constant.HeaderConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 网关下游上下文注入过滤器
 * <p>
 * 在官方 {@code SaReactorFilter} 完成鉴权之后执行，从已校验的 Sa-Token 会话读取
 * 用户身份、角色与权限，向下游微服务注入 {@code X-User-Id} / {@code X-User-Name} /
 * {@code X-User-Roles} / {@code X-User-Permissions} 头；下游通过 UserContextInterceptor
 * 还原为 {@code UserContextHolder} 上下文，供 {@code @RequiresPermissions} 切面判权。
 * </p>
 * <p>
 * 本过滤器<b>不做登录校验</b>（校验由 SaReactorFilter 负责），仅做上下文透传。
 * 白名单路径（未登录态）不会注入用户头，由下游自行处理匿名访问。
 * </p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 登录时写入 Session 的键，与 wu-cloud-auth 保持一致 */
    private static final String SESSION_USERNAME = "username";
    private static final String SESSION_ROLES = "roles";
    private static final String SESSION_PERMISSIONS = "permissions";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 仅当 Sa-Token 上下文中已登录时，才向下游注入用户标识
        if (!StpUtil.isLogin()) {
            return chain.filter(exchange);
        }

        String userId = String.valueOf(StpUtil.getLoginIdAsLong());
        SaSession session = StpUtil.getSession(false);

        String username = session == null ? null : session.getString(SESSION_USERNAME);
        String roles = session == null ? null : joinCollection(session.get(SESSION_ROLES));
        String permissions = session == null ? null : joinCollection(session.get(SESSION_PERMISSIONS));

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(HeaderConstants.USER_ID, userId);
                    setIfPresent(headers, HeaderConstants.USER_NAME, username);
                    setIfPresent(headers, HeaderConstants.USER_ROLES, roles);
                    setIfPresent(headers, HeaderConstants.USER_PERMISSIONS, permissions);
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    /**
     * 写入请求头，HTTP 头仅允许 ASCII，非 ASCII 内容做 URL 编码，下游统一解码
     */
    private void setIfPresent(org.springframework.http.HttpHeaders headers, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        headers.set(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /**
     * Session 中存放的角色/权限为集合，拼接为逗号分隔字符串
     */
    private String joinCollection(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(item);
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        String str = String.valueOf(value);
        return str.isBlank() ? null : str;
    }

    @Override
    public int getOrder() {
        // 在 SaReactorFilter（默认 HIGHEST_PRECEDENCE 之后）执行，确保鉴权已完成
        return 1;
    }
}
