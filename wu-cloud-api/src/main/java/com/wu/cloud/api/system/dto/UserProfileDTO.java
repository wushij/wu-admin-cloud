package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心完整资料（与既有规范对齐 buildProfileMap / 移动端 UserProfile）
 */
@Data
public class UserProfileDTO implements Serializable {

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
}
