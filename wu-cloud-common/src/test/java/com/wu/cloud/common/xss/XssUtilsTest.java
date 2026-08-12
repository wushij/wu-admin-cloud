package com.wu.cloud.common.xss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("XssUtils 单元测试")
class XssUtilsTest {

    @Test
    @DisplayName("null / 空串原样返回")
    void nullAndEmpty_passthrough() {
        assertNull(XssUtils.clean(null));
        assertEquals("", XssUtils.clean(""));
    }

    @Test
    @DisplayName("剥离 script / iframe")
    void stripsScriptAndIframe() {
        String input = "hello<script>alert(1)</script><iframe src=x></iframe>world";
        String cleaned = XssUtils.clean(input);
        assertFalse(cleaned.toLowerCase().contains("script"));
        assertFalse(cleaned.toLowerCase().contains("iframe"));
        assertTrue(cleaned.contains("hello"));
        assertTrue(cleaned.contains("world"));
    }

    @Test
    @DisplayName("剥离 on* 事件与危险协议")
    void stripsEventsAndProtocols() {
        String input = "<a href=\"javascript:alert(1)\" onclick=\"evil()\">x</a> vbscript:go expression(alert(1))";
        String cleaned = XssUtils.clean(input);
        assertFalse(cleaned.toLowerCase().contains("javascript:"));
        assertFalse(cleaned.toLowerCase().contains("vbscript:"));
        assertFalse(cleaned.toLowerCase().contains("onclick"));
        assertFalse(cleaned.toLowerCase().contains("expression("));
    }

    @Test
    @DisplayName("普通文本不受影响")
    void plainText_unchanged() {
        assertEquals("你好 Admin", XssUtils.clean("你好 Admin"));
    }
}
