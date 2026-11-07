package com.wu.cloud.message.websocket;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权拦截器：Sa-Token 校验 Token 或解析 URL/Header 参数
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest req = servletRequest.getServletRequest();
        
        // 1. Header / Cookie 优先；H5/小程序 WebSocket 无法自定义 Header 时回退 URL 查询参数
        //    （与既有规范对齐 AuthTokenResolver.resolve(request, true)，uniapp 传 Authorization=）
        String token = req.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = req.getHeader("satoken");
        }
        if (!StringUtils.hasText(token)) {
            token = req.getParameter("Authorization");
        }
        if (!StringUtils.hasText(token)) {
            token = req.getParameter("satoken");
        }
        if (!StringUtils.hasText(token)) {
            token = req.getParameter("token");
        }
        
        Long userId = null;
        if (StringUtils.hasText(token)) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    userId = Long.parseLong(loginId.toString());
                }
            } catch (Exception ignored) {}
        }
        
        // 2. 如果当前请求已被 Gateway 处理并携带了 Header X-User-Id
        if (userId == null) {
            String headerUserId = req.getHeader("X-User-Id");
            if (StringUtils.hasText(headerUserId)) {
                try {
                    userId = Long.parseLong(headerUserId);
                } catch (Exception ignored) {}
            }
        }
        
        // 3.（已移除）此前回退 URL 参数 userId（例如 /ws/message?userId=1）可被任意伪造，
        //    构成越权/冒充漏洞。现仅接受 Sa-Token（Authorization/satoken/token）或
        //    网关鉴权后注入的 X-User-Id（HeaderCleaningFilter 已清洗客户端伪造头）。
        //    匿名/无有效令牌的连接一律拒绝握手。

        // 4. 最后尝试当前上下文 Sa-Token 登录状态
        if (userId == null && StpUtil.isLogin()) {
            try {
                userId = StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }

        if (userId != null && userId > 0) {
            attributes.put("userId", userId);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
    }
}
