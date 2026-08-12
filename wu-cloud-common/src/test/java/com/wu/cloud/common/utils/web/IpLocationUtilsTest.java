package com.wu.cloud.common.utils.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpLocationUtilsTest {

    @Test
    void loopback_isIntranet() {
        assertEquals("内网IP", IpLocationUtils.resolve("127.0.0.1"));
    }

    @Test
    void privateLan_isIntranet() {
        assertEquals("内网IP", IpLocationUtils.resolve("192.168.1.10"));
    }

    @Test
    void blank_isUnknown() {
        assertEquals("未知", IpLocationUtils.resolve(""));
    }
}
