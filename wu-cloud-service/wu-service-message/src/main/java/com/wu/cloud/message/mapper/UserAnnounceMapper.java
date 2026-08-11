package com.wu.cloud.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.message.entity.UserAnnounceDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAnnounceMapper extends BaseMapper<UserAnnounceDO> {

    @Select("SELECT COUNT(*) FROM sys_user_announce WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(Long userId);

    @Delete("DELETE FROM sys_user_announce WHERE announce_id = #{announceId}")
    int deletePhysicalByAnnounceId(@Param("announceId") Long announceId);

    @Select("SELECT user_id FROM sys_user_announce WHERE announce_id = #{announceId}")
    List<Long> selectUserIdsByAnnounceId(@Param("announceId") Long announceId);

    @Insert("""
            <script>
            INSERT INTO sys_user_announce (user_id, announce_id, is_read, create_time) VALUES
            <foreach collection="list" item="ua" separator=",">
              (#{ua.userId}, #{ua.announceId}, #{ua.isRead}, #{ua.createTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<UserAnnounceDO> records);
}
