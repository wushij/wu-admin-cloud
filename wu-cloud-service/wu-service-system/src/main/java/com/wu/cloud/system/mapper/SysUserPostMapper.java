package com.wu.cloud.system.mapper;

import com.wu.cloud.system.entity.SysUserPostDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 用户岗位关联 Mapper
 */
@Mapper
public interface SysUserPostMapper {

    @Select("SELECT user_id FROM sys_user_post WHERE post_id = #{postId}")
    List<Long> selectUserIdsByPostId(@Param("postId") Long postId);

    @Select("""
            <script>
            SELECT id, user_id, post_id FROM sys_user_post
            WHERE user_id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<SysUserPostDO> selectByUserIds(@Param("userIds") Collection<Long> userIds);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_post (user_id, post_id) VALUES
            <foreach collection="list" item="up" separator=",">
              (#{up.userId}, #{up.postId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<SysUserPostDO> list);
}
