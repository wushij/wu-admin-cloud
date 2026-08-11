package com.wu.cloud.common.core.context;

import java.util.Collections;
import java.util.Set;

/**
 * 用户上下文 ThreadLocal 持有类
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(UserContext userContext) {
        THREAD_LOCAL.set(userContext);
    }

    public static UserContext get() {
        return THREAD_LOCAL.get();
    }

    public static Long getUserId() {
        UserContext userContext = get();
        return userContext != null ? userContext.getUserId() : null;
    }

    public static String getUsername() {
        UserContext userContext = get();
        return userContext != null ? userContext.getUsername() : null;
    }

    /**
     * 当前用户角色标识集合，永不返回 null
     */
    public static Set<String> getRoles() {
        UserContext userContext = get();
        if (userContext == null || userContext.getRoles() == null) {
            return Collections.emptySet();
        }
        return userContext.getRoles();
    }

    /**
     * 当前用户权限标识集合，永不返回 null
     */
    public static Set<String> getPermissions() {
        UserContext userContext = get();
        if (userContext == null || userContext.getPermissions() == null) {
            return Collections.emptySet();
        }
        return userContext.getPermissions();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
