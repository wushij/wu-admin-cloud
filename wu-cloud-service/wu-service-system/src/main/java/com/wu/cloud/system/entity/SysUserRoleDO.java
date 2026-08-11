package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联实体（sys_user_role）
 * <p>
 * 用于角色分配场景的批量写入与删除。
 */
@Data
@TableName("sys_user_role")
public class SysUserRoleDO implements Serializable {

    @TableId
    private Long id;
    private Long userId;
    private Long roleId;
}
