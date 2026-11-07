package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 个人信息响应（对齐移动端 UserProfile / 旧版 buildProfileMap）
 */
@Data
public class ProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String avatar;
    private Integer status;
    private Integer sex;
    private Long deptId;
    private String deptName;
    private String remark;
    private List<Long> roleIds;
    private List<String> roleNames;
    private List<String> roleCodes;
    private List<Long> postIds;
    private List<String> postNames;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private String lastLoginLocation;
    private Integer minPasswordLength;

    /** 角色标识集合（登录态兼容） */
    private Set<String> roles;

    /** 权限标识集合（登录态兼容） */
    private Set<String> permissions;
}
