package com.wu.cloud.auth.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 权限与角色加载接口实现类
 * <p>
 * 登录时 {@code AuthServiceImpl.login} 已将 {@code roles} / {@code permissions}
 * 写入 Sa-Token Session，这里直接从 Session 读取，避免每次鉴权都回查 DB。
 * <p>
 * <b>注意</b>：管理员重新分配角色 / 菜单后，旧 Session 中的权限不会自动刷新，
 * 需用户重新登录，或由管理端调用 {@code StpUtil.logout(userId)} 强制下线后再登录。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final String SESSION_KEY_PERMISSIONS = "permissions";
    private static final String SESSION_KEY_ROLES = "roles";

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = getSessionSafely(loginId);
        if (session == null) {
            return Collections.emptyList();
        }
        Object obj = session.get(SESSION_KEY_PERMISSIONS);
        if (obj instanceof Set<?> set) {
            return new ArrayList<>((Set<String>) set);
        }
        if (obj instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = getSessionSafely(loginId);
        if (session == null) {
            return Collections.emptyList();
        }
        Object obj = session.get(SESSION_KEY_ROLES);
        if (obj instanceof Set<?> set) {
            return new ArrayList<>((Set<String>) set);
        }
        if (obj instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    /**
     * 安全获取 Session：loginId 异常或会话不存在时返回 null，避免鉴权链路抛错。
     */
    private SaSession getSessionSafely(Object loginId) {
        try {
            if (loginId == null) {
                return null;
            }
            return StpUtil.getSessionByLoginId(loginId);
        } catch (Exception e) {
            return null;
        }
    }
}
