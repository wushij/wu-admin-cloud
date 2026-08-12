package com.wu.cloud.gateway.filter;

import com.wu.cloud.common.core.constant.HeaderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("HeaderCleaningFilter 单元测试")
class HeaderCleaningFilterTest {

    private final HeaderCleaningFilter filter = new HeaderCleaningFilter();

    @Test
    @DisplayName("清除客户端伪造的用户头并注入 TraceId")
    void stripsSpoofedHeaders_andInjectsTraceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/system/user/list")
                        .header(HeaderConstants.USER_ID, "999")
                        .header(HeaderConstants.USER_NAME, "hacker")
                        .header(HeaderConstants.USER_ROLES, "super_admin")
                        .header(HeaderConstants.USER_PERMISSIONS, "*")
                        .header(HeaderConstants.TRACE_ID, "client-trace")
                        .header("X-Custom", "keep")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ServerWebExchange mutated = captor.getValue();
        assertNull(mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_ID));
        assertNull(mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_NAME));
        assertNull(mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_ROLES));
        assertNull(mutated.getRequest().getHeaders().getFirst(HeaderConstants.USER_PERMISSIONS));
        assertEquals("keep", mutated.getRequest().getHeaders().getFirst("X-Custom"));
        String traceId = mutated.getRequest().getHeaders().getFirst(HeaderConstants.TRACE_ID);
        assertNotNull(traceId);
        assertFalse(traceId.isBlank());
        assertNotEquals("client-trace", traceId);
    }
}
