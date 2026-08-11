package com.wu.cloud.infra.security;

import cn.dev33.satoken.stp.StpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 会话查询（对齐单体 TokenService，供在线用户统计）
 */
@Slf4j
@Service
public class TokenService {

    public static final String SESSION_USERNAME = "username";

    @Value("${sa-token.timeout:2592000}")
    private long tokenTimeoutSeconds;

    @Data
    public static class LoginInfo {
        private Long userId;
        private String username;
        private String token;
        private Long createTime;
        private Long expireTime;
    }

    public LoginInfo getLoginInfo(Long userId) {
        try {
            String token = resolveTokenByLoginId(userId);
            if (!StringUtils.hasText(token)) {
                return null;
            }
            LoginInfo loginInfo = new LoginInfo();
            loginInfo.setUserId(userId);
            loginInfo.setToken(token);
            Object username = StpUtil.getSessionByLoginId(userId).get(SESSION_USERNAME);
            loginInfo.setUsername(username != null ? username.toString() : "");
            long timeout = StpUtil.getTokenTimeout(token);
            long now = System.currentTimeMillis();
            loginInfo.setCreateTime(now);
            loginInfo.setExpireTime(timeout > 0 ? now + timeout * 1000L : now);
            return loginInfo;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveTokenByLoginId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            List<String> tokens = StpUtil.getTokenValueListByLoginId(userId);
            if (tokens != null) {
                for (String t : tokens) {
                    if (StringUtils.hasText(t) && StpUtil.getLoginIdByToken(t) != null) {
                        return t;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("getTokenValueListByLoginId({}) failed: {}", userId, e.getMessage());
        }
        try {
            String token = StpUtil.getTokenValueByLoginId(userId);
            if (StringUtils.hasText(token)) {
                return token;
            }
        } catch (Exception e) {
            log.debug("getTokenValueByLoginId({}) failed: {}", userId, e.getMessage());
        }
        return null;
    }

    public void removeToken(Long userId) {
        try {
            StpUtil.logout(userId);
        } catch (Exception e) {
            log.debug("removeToken for user {} failed: {}", userId, e.getMessage());
        }
    }

    public List<String> listActiveTokens() {
        List<String> tokens = new ArrayList<>();
        try {
            List<String> found = StpUtil.searchTokenValue("", 0, -1, false);
            if (found != null) {
                tokens.addAll(found);
            }
        } catch (Exception e) {
            log.debug("searchTokenValue failed: {}", e.getMessage());
        }
        return tokens;
    }

    public List<Long> listActiveUserIds() {
        Set<Long> ids = new LinkedHashSet<>();
        try {
            List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
            if (sessionIds != null) {
                for (String sid : sessionIds) {
                    if (!StringUtils.hasText(sid)) {
                        continue;
                    }
                    try {
                        ids.add(Long.parseLong(sid.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("searchSessionId failed: {}", e.getMessage());
        }
        if (ids.isEmpty()) {
            for (String token : listActiveTokens()) {
                Long userId = getUserId(token);
                if (userId != null) {
                    ids.add(userId);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    public Long getUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public long tokenTimeoutMinutes() {
        return Math.max(10L, tokenTimeoutSeconds / 60L);
    }
}
