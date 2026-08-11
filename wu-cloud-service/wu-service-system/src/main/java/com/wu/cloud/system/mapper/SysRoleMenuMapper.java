package com.wu.cloud.system.mapper;

import com.wu.cloud.system.entity.SysRoleMenuDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联 Mapper
 * <p>
 * 支撑角色菜单分配（getMenuIdsForAssign / assignMenu）的读 / 写。
 */
@Mapper
public interface SysRoleMenuMapper {

    /**
     * 查询角色已分配的菜单主键集合
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除某角色的全部菜单关联（菜单重新分配前清场）
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量新增角色-菜单关联（角色授权场景）
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_menu (role_id, menu_id) VALUES
            <foreach collection="list" item="rm" separator=",">
              (#{rm.roleId}, #{rm.menuId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<SysRoleMenuDO> records);
}
