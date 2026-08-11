package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单与权限数据实体 DO（主键沿用 BaseEntity IdType.AUTO）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenuDO extends BaseEntity {

    private String name;
    private Long parentId;
    private Integer sort;
    private String path;
    private String component;
    private Integer type;
    private String permission;
    private String icon;
    private Integer status;

    /** 单体 sys_menu 表无此列，仅内存/DTO 使用 */
    @TableField(exist = false)
    private Integer visible;

    /** 单体 sys_menu 表无此列，仅内存/DTO 使用 */
    @TableField(exist = false)
    private Boolean keepAlive;
}