package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

/**
 * 系统用户 DO 实体（主键沿用 BaseEntity IdType.AUTO，勿再声明 @TableId）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserDO extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String mobile;
    private String email;

    /** 性别，数据库列为 gender：0 未知 1 男 2 女 */
    @TableField("gender")
    private Integer sex;

    private String avatar;

    /** 状态：0 禁用 1 启用 2 待审核 3 审核未通过 */
    private Integer status;

    private Long deptId;
    private String remark;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 角色ID列表 */
    @TableField(exist = false)
    private Set<Long> roleIds;

    @TableField(exist = false)
    private String postNames;

    @TableField(exist = false)
    private List<Long> postIds;

    @TableField(exist = false)
    private Boolean loginLocked;

    @TableField(exist = false)
    private Long loginLockRemainSeconds;

    @TableField(exist = false)
    private Integer loginFailCount;

    @TableField(exist = false)
    private String loginRecentIp;

    @TableField(exist = false)
    private Boolean loginIpLocked;

    @TableField(exist = false)
    private Long loginIpLockRemainSeconds;

    @TableField(exist = false)
    private Integer loginIpFailCount;
}
