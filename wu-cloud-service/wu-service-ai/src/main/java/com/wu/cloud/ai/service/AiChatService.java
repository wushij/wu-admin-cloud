package com.wu.cloud.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.ai.dto.AiChatPageDTO;
import com.wu.cloud.ai.dto.AiChatStreamReqVO;
import com.wu.cloud.ai.entity.AiChatLogDO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiChatService {
    Page<AiChatLogDO> getChatLogPage(AiChatPageDTO pageDTO);
    AiChatLogDO getChatLogById(Long id);

    /** 流式对话（SSE，事件: delta/status/done/error），返回 SseEmitter */
    SseEmitter stream(AiChatStreamReqVO reqVO, Long userId, String username);

    /** 我的历史会话列表（按 conversationId 分组，最近优先，限 30 条） */
    List<Map<String, Object>> listConversations();

    /** 指定会话的问答序列（仅本人，按 id 升序，限 50 条） */
    List<AiChatLogDO> listHistory(String conversationId);

    /** 删除整个会话（仅本人该 conversationId 的所有日志） */
    boolean deleteConversation(String conversationId);

    /** 重命名会话（更新该会话最新一条记录的 question 为 title） */
    boolean renameConversation(String conversationId, String title);
}
