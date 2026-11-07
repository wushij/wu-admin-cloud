package com.wu.cloud.common.utils.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 解析真实客户端 IP 工具类。
 *
 * <p>只有当请求来自可信代理时才读取 {@code X-Real-IP} / {@code X-Forwarded-For} /
 * {@code CF-Connecting-IP}，否则一律使用 TCP 对端地址。
 * 判定逻辑统一收敛在 {@link TrustedProxyIpResolver}，与网关侧共用同一套口径。</p>
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return TrustedProxyIpResolver.resolve(
                request.getRemoteAddr(),
                request.getHeader("X-Real-IP"),
                request.getHeader("X-Forwarded-For"),
                request.getHeader("CF-Connecting-IP"));
    }
}
