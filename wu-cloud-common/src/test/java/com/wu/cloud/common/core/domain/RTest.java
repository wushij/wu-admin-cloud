package com.wu.cloud.common.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("统一响应 R 单元测试")
class RTest {

    @Test
    @DisplayName("ok：默认成功码 200")
    void ok_successCode() {
        R<String> r = R.ok("data");
        assertEquals(200, r.getCode());
        assertEquals("data", r.getData());
        assertTrue(r.isSuccess());
    }

    @Test
    @DisplayName("fail(code,msg)：保留业务错误码")
    void fail_withCode() {
        R<Void> r = R.fail(403, "无权限");
        assertEquals(403, r.getCode());
        assertEquals("无权限", r.getMsg());
        assertFalse(r.isSuccess());
    }

    @Test
    @DisplayName("fail(msg)：默认失败码 500")
    void fail_defaultCode() {
        R<Void> r = R.fail("系统繁忙，请稍后重试");
        assertEquals(500, r.getCode());
        assertEquals("系统繁忙，请稍后重试", r.getMsg());
    }
}
