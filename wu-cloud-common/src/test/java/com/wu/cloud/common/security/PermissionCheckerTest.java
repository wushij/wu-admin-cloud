package com.wu.cloud.common.security;

import com.wu.cloud.common.core.context.UserContext;
import com.wu.cloud.common.core.context.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionChecker 单元测试")
class PermissionCheckerTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("无上下文：非超管且无权限")
    void noContext_denied() {
        assertFalse(PermissionChecker.isSuperAdmin());
        assertFalse(PermissionChecker.hasPermission("system:user:query"));
        assertFalse(PermissionChecker.hasRole("admin"));
    }

    @Test
    @DisplayName("通配符权限：视为超管")
    void allPermission_isSuperAdmin() {
        UserContextHolder.set(UserContext.builder()
                .userId(1L)
                .permissions(Set.of(PermissionChecker.ALL_PERMISSION))
                .build());
        assertTrue(PermissionChecker.isSuperAdmin());
        assertTrue(PermissionChecker.hasPermission("any:perm"));
    }

    @Test
    @DisplayName("super_admin 角色：视为超管")
    void superAdminRole_isSuperAdmin() {
        UserContextHolder.set(UserContext.builder()
                .userId(1L)
                .roles(Set.of(PermissionChecker.SUPER_ADMIN_ROLE))
                .build());
        assertTrue(PermissionChecker.isSuperAdmin());
        assertTrue(PermissionChecker.hasRole("other"));
    }

    @Test
    @DisplayName("普通用户：按权限集合判定")
    void normalUser_checksPermissionSet() {
        UserContextHolder.set(UserContext.builder()
                .userId(2L)
                .permissions(Set.of("system:user:query"))
                .roles(Set.of("common"))
                .build());
        assertTrue(PermissionChecker.hasPermission("system:user:query"));
        assertFalse(PermissionChecker.hasPermission("system:user:create"));
        assertTrue(PermissionChecker.hasRole("common"));
        assertFalse(PermissionChecker.hasRole("admin"));
    }

    @Test
    @DisplayName("空白权限/角色：视为通过")
    void blankPermissionOrRole_passes() {
        assertTrue(PermissionChecker.hasPermission(""));
        assertTrue(PermissionChecker.hasRole(null));
    }
}
