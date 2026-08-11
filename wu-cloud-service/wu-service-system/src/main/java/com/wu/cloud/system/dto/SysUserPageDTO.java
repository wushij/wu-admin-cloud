package com.wu.cloud.system.dto;

import com.wu.cloud.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPageDTO extends PageQuery {
    private String username;
    private String nickname;
    private String mobile;
    private Integer status;
    private Long deptId;
    private Long postId;
    /** 仅筛选登录失败临时锁定的账号 */
    private Boolean loginLocked;
}
