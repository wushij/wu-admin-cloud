package com.wu.cloud.common.security.aspect;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.common.security.annotation.Logical;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.common.security.annotation.RequiresRoles;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 权限校验切面：拦截 {@link RequiresPermissions} 与 {@link RequiresRoles} 注解
 */
@Aspect
public class PermissionAspect {

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;

    @Before("@annotation(com.wu.cloud.common.security.annotation.RequiresPermissions) "
            + "|| @within(com.wu.cloud.common.security.annotation.RequiresPermissions)")
    public void checkPermission(JoinPoint joinPoint) {
        RequiresPermissions annotation = resolveAnnotation(joinPoint, RequiresPermissions.class);
        if (annotation == null) {
            return;
        }
        requireLogin();

        String[] permissions = annotation.value();
        if (permissions.length == 0) {
            return;
        }

        boolean pass = annotation.logical() == Logical.AND
                ? allMatch(permissions, true)
                : anyMatch(permissions, true);

        if (!pass) {
            throw new BusinessException(FORBIDDEN, "无权限访问，缺少权限：" + String.join(",", permissions));
        }
    }

    @Before("@annotation(com.wu.cloud.common.security.annotation.RequiresRoles) "
            + "|| @within(com.wu.cloud.common.security.annotation.RequiresRoles)")
    public void checkRole(JoinPoint joinPoint) {
        RequiresRoles annotation = resolveAnnotation(joinPoint, RequiresRoles.class);
        if (annotation == null) {
            return;
        }
        requireLogin();

        String[] roles = annotation.value();
        if (roles.length == 0) {
            return;
        }

        boolean pass = annotation.logical() == Logical.AND
                ? allMatch(roles, false)
                : anyMatch(roles, false);

        if (!pass) {
            throw new BusinessException(FORBIDDEN, "无权限访问，缺少角色：" + String.join(",", roles));
        }
    }

    private void requireLogin() {
        if (UserContextHolder.get() == null || UserContextHolder.getUserId() == null) {
            throw new BusinessException(UNAUTHORIZED, "未登录或登录已过期");
        }
    }

    private boolean anyMatch(String[] values, boolean isPermission) {
        for (String value : values) {
            if (isPermission ? PermissionChecker.hasPermission(value) : PermissionChecker.hasRole(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean allMatch(String[] values, boolean isPermission) {
        for (String value : values) {
            if (!(isPermission ? PermissionChecker.hasPermission(value) : PermissionChecker.hasRole(value))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 优先取方法上的注解，其次取类上的注解
     */
    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(JoinPoint joinPoint, Class<A> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        A annotation = method.getAnnotation(type);
        if (annotation != null) {
            return annotation;
        }
        return joinPoint.getTarget().getClass().getAnnotation(type);
    }
}
