package com.wu.cloud.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.message.entity.ChatGroupMemberDO;
import com.wu.cloud.message.vo.ChatGroupMemberCountVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMemberDO> {

    @Select("""
            <script>
            SELECT group_id AS groupId, COUNT(*) AS memberCount
            FROM sys_chat_group_member
            WHERE group_id IN
            <foreach collection="groupIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            GROUP BY group_id
            </script>
            """)
    List<ChatGroupMemberCountVO> selectMemberCountByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    @Insert("""
            <script>
            INSERT INTO sys_chat_group_member (group_id, user_id, role, join_time) VALUES
            <foreach collection="list" item="m" separator=",">
              (#{m.groupId}, #{m.userId}, #{m.role}, #{m.joinTime})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("list") List<ChatGroupMemberDO> members);
}
