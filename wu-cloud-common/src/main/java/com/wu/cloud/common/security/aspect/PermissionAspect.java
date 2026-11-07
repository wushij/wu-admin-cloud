package com.wu.cloud.common.security.aspect;

import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.common.security.annotation.Logical;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.common.security.annotation.RequiresRoles;
import com.wu.cloud.common.security.risk.RiskPrincipal;
import com.wu.cloud.common.security.risk.RiskReporter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验切面：拦截 {@link RequiresPermissions} 与 {@link RequiresRoles} 注解
 */
@Aspect
public class PermissionAspect {

    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;

    /** 越权访问的风险分增量（与既有风控口径一致） */
    private static final int PERMISSION_DENIED_RISK_DELTA = 15;

    /** 风控上报器；未接入时为空，跳过上报 */
    private final ObjectProvider<RiskReporter> riskReporterProvider;

    public PermissionAspect() {
        this(null);
    }

    public PermissionAspect(ObjectProvider<RiskReporter> riskReporterProvider) {
        this.riskReporterProvider = riskReporterProvider;
    }

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
            String reason = "无权限访问，缺少权限：" + String.join(",", permissions);
            reportPermissionDenied(reason);
            throw new BusinessException(FORBIDDEN, reason);
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

    /**
     * 越权访问上报风控：由风控属主统一计分，累加到封禁阈值时自动封禁。
     * 上报失败不影响业务语义（仍按 403 拒绝）。
     */
    private void reportPermissionDenied(String reason) {
        if (riskReporterProvider == null) {
            return;
        }
        RiskReporter reporter = riskReporterProvider.getIfAvailable();
        if (reporter == null) {
            return;
        }
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
            String ip = resolveClientIp(request);
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String userAgent = request.getHeader("User-Agent");
            reporter.report("PERMISSION_DENIED", RiskPrincipal.ofIp(ip).key(), ip,
                    PERMISSION_DENIED_RISK_DELTA, "BLOCKED", uri, method, userAgent, reason);
        } catch (Exception ignored) {
            // 风控上报属于旁路动作，异常不得影响鉴权结果
        }
    }

    /** 取真实客户端 IP：优先 X-Forwarded-For，其次 X-Real-IP，最后 remoteAddr */
    private String resolveClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            if (!first.isBlank()) {
                return first.trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "" : remote;
    }
}
