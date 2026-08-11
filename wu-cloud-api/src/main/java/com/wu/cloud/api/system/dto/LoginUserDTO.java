package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 登录用户跨服务 DTO（含鉴权所需的密码与权限信息）
 */
@Data
public class LoginUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主键 */
    private Long userId;

    /** 账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 密码密文，仅内部 RPC 传输 */
    private String password;

    /** 状态：0 正常 1 停用 */
    private Integer status;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 头像 */
    private String avatar;

    /** 性别 */
    private Integer sex;

    /** 部门主键 */
    private Long deptId;

    /** 备注/个人简介 */
    private String remark;

    /** 权限标识集合 */
    private Set<String> permissions;

    /** 角色标识集合 */
    private Set<String> roles;
}
