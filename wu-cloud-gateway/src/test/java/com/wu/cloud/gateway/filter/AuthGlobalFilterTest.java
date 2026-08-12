package com.wu.cloud.gateway.filter;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.common.core.constant.HeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthGlobalFilter 单元测试")
class AuthGlobalFilterTest {

    private final AuthGlobalFilter filter = new AuthGlobalFilter();

    @Test
    @DisplayName("认证中心路径：跳过用户头注入，避免 GlobalFilter 阶段 SaTokenContextException")
    void authPath_skipsInjection() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login")
                        .header("Authorization", "stale-token")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("未登录：不注入用户头，直接放行")
    void notLogin_forwardsWithoutUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/dept/tree").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("GlobalFilter 阶段 isLogin=false 时从 Authorization 头解析 Token 并注入用户头")
    void tokenHeader_injectsUserHeadersWhenNotInReactorContext() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/dept/tree")
                        .header("Authorization", "token-abc")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        SaSession session = mock(SaSession.class);
        when(session.getString("username")).thenReturn("admin");
        when(session.get("roles")).thenReturn(List.of("super_admin"));
        when(session.get("permissions")).thenReturn(List.of("system:dept:list"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getLoginIdByToken("token-abc")).thenReturn(1L);
            stp.when(() -> StpUtil.getSessionByLoginId(1L, false)).thenReturn(session);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }

        ServerWebExchange mutated = captor.getValue();
        assertEquals("1", mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_ID));
        assertEquals("admin", URLDecoder.decode(
                mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_NAME), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Token 有效：注入 userId/roles/permissions")
    void loggedIn_injectsUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/user/list")
                        .header("Authorization", "token-xyz")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        SaSession session = mock(SaSession.class);
        when(session.getString("username")).thenReturn("alice");
        when(session.get("roles")).thenReturn(List.of("admin", "ops"));
        when(session.get("permissions")).thenReturn(List.of("system:user:list"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getLoginIdByToken("token-xyz")).thenReturn(42L);
            stp.when(() -> StpUtil.getSessionByLoginId(42L, false)).thenReturn(session);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }

        ServerWebExchange mutated = captor.getValue();
        assertEquals("42", mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_ID));
        assertEquals("alice", URLDecoder.decode(
                mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_NAME), StandardCharsets.UTF_8));
        assertEquals("admin,ops", URLDecoder.decode(
                mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_ROLES), StandardCharsets.UTF_8));
        assertEquals("system:user:list", URLDecoder.decode(
                mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_PERMISSIONS), StandardCharsets.UTF_8));
        assertNotSame(exchange, mutated);
    }

    @Test
    @DisplayName("有昵称时 X-User-Name 注入昵称而非账号")
    void nicknamePreferredOverUsername() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/config-group/update")
                        .header("Authorization", "token-nick")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        SaSession session = mock(SaSession.class);
        when(session.getString("nickname")).thenReturn("吴77");
        when(session.getString("username")).thenReturn("admin");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getLoginIdByToken("token-nick")).thenReturn(1L);
            stp.when(() -> StpUtil.getSessionByLoginId(1L, false)).thenReturn(session);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }

        ServerWebExchange mutated = captor.getValue();
        assertEquals("吴77", URLDecoder.decode(
                mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_NAME), StandardCharsets.UTF_8));
    }
}
