package com.wu.cloud.system.mapper;

import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysDictDataDO;
import com.wu.cloud.system.entity.SysDictTypeDO;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 回收站 Mapper（system 服务内）
 *
 * <p><b>为什么不用 LambdaQueryWrapper：</b>
 * 实体继承 {@code BaseEntity}，其 {@code deleted} 字段标注了 {@code @TableLogic}，
 * MyBatis-Plus 会为所有 Wrapper 查询自动追加 {@code AND deleted = 0}。
 * 若再手工拼 {@code .eq(deleted, 1)}，最终 SQL 变成
 * {@code deleted = 1 AND deleted = 0}，条件恒不成立、永远返回空列表。
 * 因此回收站相关读写一律使用原生 SQL 绕过逻辑删除拦截。</p>
 *
 * <p><b>为什么在 SQL 里做分页：</b>
 * 当前 {@code SystemMyBatisPlusConfig} 未注册 {@code PaginationInnerInterceptor}，
 * {@code selectPage} 不会真正分页。此处用 LIMIT 在数据库层完成，避免全表加载。</p>
 *
 * <p>各表名称列已逐一核对：sys_user.username、sys_role.name、sys_menu.name、
 * sys_dept.name、sys_dict_type.name、sys_dict_data.label。</p>
 */
@Mapper
public interface SysRecycleMapper {

    // ==================== 用户 sys_user ====================

    @Select("<script>SELECT * FROM sys_user WHERE deleted = 1"
            + "<if test='username != null and username != \"\"'> AND username LIKE CONCAT('%', #{username}, '%')</if>"
            + "<if test='mobile != null and mobile != \"\"'> AND mobile LIKE CONCAT('%', #{mobile}, '%')</if>"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='deptId != null'> AND dept_id = #{deptId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysUserDO> selectDeletedUsers(@Param("keyword") String keyword,
                                       @Param("username") String username,
                                       @Param("mobile") String mobile,
                                       @Param("status") Integer status,
                                       @Param("deptId") Long deptId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_user WHERE deleted = 1"
            + "<if test='username != null and username != \"\"'> AND username LIKE CONCAT('%', #{username}, '%')</if>"
            + "<if test='mobile != null and mobile != \"\"'> AND mobile LIKE CONCAT('%', #{mobile}, '%')</if>"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='deptId != null'> AND dept_id = #{deptId}</if>"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></script>")
    long countDeletedUsers(@Param("keyword") String keyword,
                           @Param("username") String username,
                           @Param("mobile") String mobile,
                           @Param("status") Integer status,
                           @Param("deptId") Long deptId);

    @Update("UPDATE sys_user SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreUser(@Param("id") Long id);

    @Delete("DELETE FROM sys_user WHERE id = #{id} AND deleted = 1")
    int purgeUser(@Param("id") Long id);

    // ==================== 角色 sys_role ====================

    @Select("<script>SELECT * FROM sys_role WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysRoleDO> selectDeletedRoles(@Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_role WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (name LIKE CONCAT('%', #{keyword}, '%') OR code LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></script>")
    long countDeletedRoles(@Param("keyword") String keyword);

    @Update("UPDATE sys_role SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreRole(@Param("id") Long id);

    @Delete("DELETE FROM sys_role WHERE id = #{id} AND deleted = 1")
    int purgeRole(@Param("id") Long id);

    // ==================== 菜单 sys_menu ====================

    @Select("<script>SELECT * FROM sys_menu WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysMenuDO> selectDeletedMenus(@Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_menu WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + "</script>")
    long countDeletedMenus(@Param("keyword") String keyword);

    @Update("UPDATE sys_menu SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreMenu(@Param("id") Long id);

    @Delete("DELETE FROM sys_menu WHERE id = #{id} AND deleted = 1")
    int purgeMenu(@Param("id") Long id);

    // ==================== 部门 sys_dept ====================

    @Select("<script>SELECT * FROM sys_dept WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysDeptDO> selectDeletedDepts(@Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_dept WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'> AND name LIKE CONCAT('%', #{keyword}, '%')</if>"
            + "</script>")
    long countDeletedDepts(@Param("keyword") String keyword);

    @Update("UPDATE sys_dept SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreDept(@Param("id") Long id);

    @Delete("DELETE FROM sys_dept WHERE id = #{id} AND deleted = 1")
    int purgeDept(@Param("id") Long id);

    // ==================== 字典类型 sys_dict_type ====================

    @Select("<script>SELECT * FROM sys_dict_type WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (dict_name LIKE CONCAT('%', #{keyword}, '%') OR dict_type LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysDictTypeDO> selectDeletedDictTypes(@Param("keyword") String keyword,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_dict_type WHERE deleted = 1"
            + "<if test='keyword != null and keyword != \"\"'>"
            + " AND (dict_name LIKE CONCAT('%', #{keyword}, '%') OR dict_type LIKE CONCAT('%', #{keyword}, '%'))"
            + "</if></script>")
    long countDeletedDictTypes(@Param("keyword") String keyword);

    @Update("UPDATE sys_dict_type SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreDictType(@Param("id") Long id);

    @Delete("DELETE FROM sys_dict_type WHERE id = #{id} AND deleted = 1")
    int purgeDictType(@Param("id") Long id);

    // ==================== 字典数据 sys_dict_data ====================

    @Select("<script>SELECT * FROM sys_dict_data WHERE deleted = 1"
            + "<if test='dictType != null and dictType != \"\"'> AND dict_type = #{dictType}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND dict_label LIKE CONCAT('%', #{keyword}, '%')</if>"
            + " ORDER BY update_time DESC LIMIT #{offset}, #{limit}</script>")
    List<SysDictDataDO> selectDeletedDictData(@Param("dictType") String dictType,
                                              @Param("keyword") String keyword,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    @Select("<script>SELECT COUNT(*) FROM sys_dict_data WHERE deleted = 1"
            + "<if test='dictType != null and dictType != \"\"'> AND dict_type = #{dictType}</if>"
            + "<if test='keyword != null and keyword != \"\"'> AND dict_label LIKE CONCAT('%', #{keyword}, '%')</if>"
            + "</script>")
    long countDeletedDictData(@Param("dictType") String dictType, @Param("keyword") String keyword);

    @Update("UPDATE sys_dict_data SET deleted = 0, update_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreDictData(@Param("id") Long id);

    @Delete("DELETE FROM sys_dict_data WHERE id = #{id} AND deleted = 1")
    int purgeDictData(@Param("id") Long id);

    // ==================== 汇总计数（供 /api/system/recycle/summary）====================

    @Select("SELECT COUNT(*) FROM sys_post WHERE deleted = 1")
    long countDeletedPosts();

    // ==================== 系统配置分组 sys_config_group ====================

    @Select("SELECT COUNT(*) FROM sys_config_group WHERE deleted = 1")
    long countDeletedConfigGroups();

    // 注意：sys_notice 表位于 wu_ticket 库（见 sql/03_wu_ticket.sql），不在本服务数据源内，
    // 不可在此聚合，其计数由 ticket 服务自行提供。
}
