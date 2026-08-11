package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-菜单关联实体（sys_role_menu）
 * <p>
 * 用于角色菜单分配场景的批量写入与删除。
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenuDO implements Serializable {

    @TableId
    private Long id;
    private Long roleId;
    private Long menuId;
}
