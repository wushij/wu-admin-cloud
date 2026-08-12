package com.wu.cloud.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.common.core.constant.HeaderConstants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 网关下游上下文注入过滤器
 * <p>
 * {@code SaReactorFilter} 为 WebFilter，{@code StpUtil.isLogin()} 在 GlobalFilter 阶段常不可用；
 * 与单体「鉴权通过后业务侧能读到登录用户」对齐：从请求 Token（Header/Cookie）解析 loginId，
 * 向下游注入 {@code X-User-Id} / {@code X-User-Name} / {@code X-User-Roles} / {@code X-User-Permissions}。
 * </p>
 * <p>
 * {@code /api/auth/**} 由认证中心自行鉴权，本过滤器跳过，避免在 GlobalFilter 阶段调用
 * {@code StpUtil} 触发 {@code SaTokenContextException}（表现为登录/登出后偶发 503）。
 * </p>
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_NAME = "Authorization";

    /** 登录时写入 Session 的键，与 wu-cloud-auth 保持一致 */
    private static final String SESSION_USERNAME = "username";
    private static final String SESSION_NICKNAME = "nickname";
    private static final String SESSION_ROLES = "roles";
    private static final String SESSION_PERMISSIONS = "permissions";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (shouldSkipUserInjection(path)) {
            return chain.filter(exchange);
        }

        ResolvedLogin login = resolveLogin(exchange.getRequest());
        if (login == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.set(HeaderConstants.USER_ID, String.valueOf(login.userId()));
                    setIfPresent(headers, HeaderConstants.USER_NAME, login.username());
                    setIfPresent(headers, HeaderConstants.USER_ROLES, login.roles());
                    setIfPresent(headers, HeaderConstants.USER_PERMISSIONS, login.permissions());
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean shouldSkipUserInjection(String path) {
        return path != null && path.startsWith("/api/auth/");
    }

    /**
     * GlobalFilter 阶段无 Sa-Token Reactor 上下文，仅通过 Token 查 Redis 会话注入下游头。
     */
    private ResolvedLogin resolveLogin(ServerHttpRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            long userId = Long.parseLong(String.valueOf(loginId));
            SaSession session = StpUtil.getSessionByLoginId(loginId, false);
            return new ResolvedLogin(
                    userId,
                    resolveDisplayName(session),
                    session == null ? null : joinCollection(session.get(SESSION_ROLES)),
                    session == null ? null : joinCollection(session.get(SESSION_PERMISSIONS))
            );
        } catch (NotLoginException | SaTokenContextException | NumberFormatException ignored) {
            return null;
        }
    }

    /** 与单体 SecurityUtils.getLoginUserDisplayName 一致：昵称优先，无昵称回退账号 */
    private String resolveDisplayName(SaSession session) {
        if (session == null) {
            return null;
        }
        String nickname = session.getString(SESSION_NICKNAME);
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        String username = session.getString(SESSION_USERNAME);
        return StringUtils.hasText(username) ? username.trim() : null;
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(TOKEN_NAME);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        HttpCookie cookie = request.getCookies().getFirst(TOKEN_NAME);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue().trim();
        }
        return null;
    }

    private void setIfPresent(org.springframework.http.HttpHeaders headers, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        headers.set(name, URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

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
        return 1;
    }

    private record ResolvedLogin(long userId, String username, String roles, String permissions) {
    }
}
