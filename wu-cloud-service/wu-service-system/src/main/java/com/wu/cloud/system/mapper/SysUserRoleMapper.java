package com.wu.cloud.system.mapper;

import com.wu.cloud.system.entity.SysUserRoleDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户角色关联 Mapper
 * <p>
 * 既支撑登录鉴权链路（查角色编码 / 权限标识），也支撑角色分配写入（insertBatch / deleteByUserId）。
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 查询用户拥有的角色编码集合（仅启用状态）
     */
    @Select("""
            SELECT r.code FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(Long userId);

    /**
     * 查询用户拥有的角色主键集合
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 查询用户拥有的权限标识集合（角色 - 菜单）
     */
    @Select("""
            SELECT DISTINCT m.permission FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            INNER JOIN sys_role_menu rm ON rm.role_id = r.id
            INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.status = 1 AND m.deleted = 0
            WHERE ur.user_id = #{userId}
              AND m.permission IS NOT NULL AND m.permission <> ''
            """)
    List<String> selectPermissionsByUserId(Long userId);

    /**
     * 删除某用户的全部角色关联（角色重新分配前清场）
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 批量新增用户-角色关联（角色分配场景）
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="list" item="ur" separator=",">
              (#{ur.userId}, #{ur.roleId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<SysUserRoleDO> records);
}
