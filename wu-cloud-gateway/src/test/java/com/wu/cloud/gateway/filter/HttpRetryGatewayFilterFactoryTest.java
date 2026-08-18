package com.wu.cloud.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HttpRetry：跳过 WebSocket 升级")
class HttpRetryGatewayFilterFactoryTest {

    @Test
    @DisplayName("Upgrade: websocket 判定为 WS 握手")
    void detectsWebSocketUpgrade() {
        MockServerWebExchange ws = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/ws/message")
                        .header("Upgrade", "websocket")
                        .header("Connection", "Upgrade")
                        .build());
        MockServerWebExchange http = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/message/chat/users").build());

        assertTrue(HttpRetryGatewayFilterFactory.isWebSocketUpgrade(ws));
        assertFalse(HttpRetryGatewayFilterFactory.isWebSocketUpgrade(http));
    }

    @Test
    @DisplayName("WS 握手直接放行，不包进 Retry")
    void websocketSkipsRetryWrapper() {
        HttpRetryGatewayFilterFactory factory = new HttpRetryGatewayFilterFactory();
        RetryGatewayFilterFactory.RetryConfig config = new RetryGatewayFilterFactory.RetryConfig();
        config.setRetries(2);
        GatewayFilter filter = factory.apply(config);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/ws/message")
                        .header("Upgrade", "websocket")
                        .header("Connection", "Upgrade")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
    }
}
