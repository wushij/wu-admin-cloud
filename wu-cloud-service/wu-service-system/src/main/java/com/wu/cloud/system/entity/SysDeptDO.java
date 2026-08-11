package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门数据实体 DO（主键沿用 BaseEntity IdType.AUTO）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDeptDO extends BaseEntity {

    private String name;
    private Long parentId;
    private Integer sort;
    private Long leaderUserId;
    private String phone;
    private String email;
    private Integer status;
}
