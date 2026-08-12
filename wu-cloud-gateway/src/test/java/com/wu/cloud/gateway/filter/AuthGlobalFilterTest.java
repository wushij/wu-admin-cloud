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
    @DisplayName("未登录：不注入用户头，直接放行")
    void notLogin_forwardsWithoutUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(false);
            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        }
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("已登录：注入 userId/roles/permissions")
    void loggedIn_injectsUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/user/list").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        SaSession session = mock(SaSession.class);
        when(session.getString("username")).thenReturn("alice");
        when(session.get("roles")).thenReturn(List.of("admin", "ops"));
        when(session.get("permissions")).thenReturn(List.of("system:user:list"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            stp.when(() -> StpUtil.getSession(false)).thenReturn(session);

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
}
