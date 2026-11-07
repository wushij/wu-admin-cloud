package com.wu.cloud.common.core.constant;

/**
 * 跨服务通信 Header 常量
 * <p>
 * 网关 {@code HeaderCleaningFilter} 会清除客户端伪造的这些头并重新注入，
 * Feign {@code FeignAuthInterceptor} 透传这些头，下游通过 {@code UserContextHolder} 还原。
 * 所有涉及这些头的代码必须引用本常量，禁止硬编码字符串。
 * </p>
 */
public final class HeaderConstants {

    private HeaderConstants() {
    }

    /** 用户 ID */
    public static final String USER_ID = "X-User-Id";

    /** 用户名 */
    public static final String USER_NAME = "X-User-Name";

    /** 用户角色（逗号分隔） */
    public static final String USER_ROLES = "X-User-Roles";

    /** 用户权限标识（逗号分隔） */
    public static final String USER_PERMISSIONS = "X-User-Permissions";

    /** 链路追踪 ID */
    public static final String TRACE_ID = "X-Trace-Id";

    /** 服务间内网调用令牌（零信任：/inner/** 端点强制校验） */
    public static final String INNER_TOKEN = "X-Inner-Token";
}
