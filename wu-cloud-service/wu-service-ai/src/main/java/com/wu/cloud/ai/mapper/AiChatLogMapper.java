package com.wu.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.ai.entity.AiChatLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiChatLogMapper extends BaseMapper<AiChatLogDO> {

    /**
     * 当前用户的历史会话列表（按 conversation_id 聚合，首问作标题，最近优先）
     * <p>严格限定 user_id = 会话用户，防止越权查看他人会话。</p>
     */
    @Select("""
            SELECT t.conversation_id AS conversationId,
                   (SELECT q.question FROM sys_ai_chat_log q
                    WHERE q.conversation_id = t.conversation_id AND q.user_id = #{userId} AND q.deleted = 0
                    ORDER BY q.id ASC LIMIT 1) AS title,
                   MAX(t.create_time) AS lastTime,
                   COUNT(*) AS messageCount
            FROM sys_ai_chat_log t
            WHERE t.user_id = #{userId} AND t.deleted = 0
              AND t.conversation_id IS NOT NULL AND t.conversation_id != ''
            GROUP BY t.conversation_id
            ORDER BY lastTime DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectConversations(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 按天聚合的 Token 使用趋势（仅统计未删除日志）
     */
    @Select("<script>"
            + "SELECT DATE(create_time) AS date, SUM(total_tokens) AS tokens, COUNT(*) AS chats "
            + "FROM sys_ai_chat_log "
            + "WHERE deleted = 0 "
            + "<if test='startDate != null and startDate != \"\"'>AND create_time &gt;= #{startDate}</if> "
            + "<if test='endDate != null and endDate != \"\"'>AND create_time &lt;= #{endDate}</if> "
            + "GROUP BY DATE(create_time) "
            + "ORDER BY date"
            + "</script>")
    List<Map<String, Object>> selectDailyTokenStats(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
