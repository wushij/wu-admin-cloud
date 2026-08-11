package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表 DO 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class RoleDO extends BaseEntity {

    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private Integer dataScope;
}
