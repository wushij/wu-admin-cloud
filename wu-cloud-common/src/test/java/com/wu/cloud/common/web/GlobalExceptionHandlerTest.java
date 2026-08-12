package com.wu.cloud.common.web;

import cn.dev33.satoken.exception.NotLoginException;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        lenient().when(request.getRequestURI()).thenReturn("/api/message/chat/send");
    }

    @Test
    @DisplayName("业务异常：透传 code/msg")
    void handleBusinessException() {
        var resp = handler.handleBusinessException(new BusinessException(400, "无法发送，存在拉黑关系"), request);
        assertEquals(400, resp.getStatusCode().value());
        R<Void> r = resp.getBody();
        assertNotNull(r);
        assertEquals(400, r.getCode());
        assertEquals("无法发送，存在拉黑关系", r.getMsg());
    }

    @Test
    @DisplayName("未登录：401")
    void handleNotLogin() {
        NotLoginException e = new NotLoginException("未登录", "-1", "not-login");
        R<Void> r = handler.handleNotLogin(e, request);
        assertEquals(401, r.getCode());
        assertTrue(r.getMsg().contains("未登录"));
    }

    @Test
    @DisplayName("非法参数：返回消息")
    void handleIllegalArgument() {
        R<Void> r = handler.handleIllegalArgument(new IllegalArgumentException("参数错误"), request);
        assertEquals(500, r.getCode());
        assertEquals("参数错误", r.getMsg());
    }

    @Test
    @DisplayName("方法不支持：405")
    void handleMethodNotSupported() {
        R<Void> r = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"), request);
        assertEquals(405, r.getCode());
        assertTrue(r.getMsg().contains("DELETE"));
    }

    @Test
    @DisplayName("未知异常：系统繁忙（避免泄露内部细节）")
    void handleException_genericBusy() {
        R<Void> r = handler.handleException(new RuntimeException("boom"), request);
        assertEquals(500, r.getCode());
        assertEquals("系统繁忙，请稍后重试", r.getMsg());
    }
}
