package com.wu.cloud.common.security;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;

import java.util.Set;

/**
 * 权限判定工具：基于当前线程上下文中的权限与角色集合进行校验
 *
 * <p>超级管理员使用通配符 {@code *:*:*} 或角色 {@code super_admin} 表示拥有全部权限。</p>
 */
public final class PermissionChecker {

    /** 超级权限通配符 */
    public static final String ALL_PERMISSION = "*:*:*";

    /** 超级管理员角色标识（对齐 sys_role.code，勿改成非库值，否则超管判断永久失效） */
    public static final String SUPER_ADMIN_ROLE = "super_admin";

    private PermissionChecker() {
    }

    /**
     * 是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            return false;
        }
        Set<String> permissions = context.getPermissions();
        if (permissions != null && permissions.contains(ALL_PERMISSION)) {
            return true;
        }
        Set<String> roles = context.getRoles();
        return roles != null && roles.contains(SUPER_ADMIN_ROLE);
    }

    /**
     * 是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        if (isSuperAdmin()) {
            return true;
        }
        UserContext context = UserContextHolder.get();
        if (context == null || context.getPermissions() == null) {
            return false;
        }
        return context.getPermissions().contains(permission);
    }

    /**
     * 是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        UserContext context = UserContextHolder.get();
        if (context == null || context.getRoles() == null) {
            return false;
        }
        if (context.getRoles().contains(SUPER_ADMIN_ROLE)) {
            return true;
        }
        return context.getRoles().contains(role);
    }
}
