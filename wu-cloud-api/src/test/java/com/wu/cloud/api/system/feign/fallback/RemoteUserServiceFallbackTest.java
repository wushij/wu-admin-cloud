package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RemoteUserServiceFallback 单元测试")
class RemoteUserServiceFallbackTest {

    private final RemoteUserServiceFallback factory = new RemoteUserServiceFallback();

    @Test
    @DisplayName("降级方法返回失败 R，不返回 null")
    void create_returnsFailResponses() {
        RemoteUserService fallback = factory.create(new RuntimeException("timeout"));

        R<?> userInfo = fallback.getUserInfoByUsername("alice");
        assertNotNull(userInfo);
        assertFalse(userInfo.isSuccess());
        assertTrue(userInfo.getMsg().contains("不可用"));

        assertFalse(fallback.getUserById(1L).isSuccess());
        assertFalse(fallback.registerUser(null).isSuccess());
        assertFalse(fallback.resetPassword(1L, "x").isSuccess());
        assertFalse(fallback.getMenusByUserId(1L).isSuccess());
        assertNull(fallback.getUserInfoByUsername("alice").getData());
    }
}
