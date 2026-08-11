package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wu.cloud.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 岗位数据实体 DO（主键沿用 BaseEntity IdType.AUTO）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPostDO extends BaseEntity {

    private Long parentId;
    private String postCode;
    private String postName;
    private Integer sort;
    private Integer status;
    private String remark;

    /** 树形结构子节点（非数据库字段） */
    @TableField(exist = false)
    private List<SysPostDO> children;
}
