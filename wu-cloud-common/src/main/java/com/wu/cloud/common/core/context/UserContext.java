package com.wu.cloud.common.core.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Set;

/**
 * 线程上下文登录用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 账号名称 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 部门 ID */
    private Long deptId;

    /** 角色标识集合 */
    private Set<String> roles;

    /** 权限标识集合 */
    private Set<String> permissions;
}
