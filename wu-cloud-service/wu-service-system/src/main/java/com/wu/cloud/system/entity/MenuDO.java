package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单表 DO 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class MenuDO extends BaseEntity {

    private String name;
    private Long parentId;
    private Integer sort;
    private String path;
    private String component;
    private Integer type;
    private Integer status;
    private String permission;
    private String icon;
    private Boolean visible;
    private Boolean keepAlive;
}
