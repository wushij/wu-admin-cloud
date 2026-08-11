package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门表 DO 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class DeptDO extends BaseEntity {

    private String name;
    private Long parentId;
    private Integer sort;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
