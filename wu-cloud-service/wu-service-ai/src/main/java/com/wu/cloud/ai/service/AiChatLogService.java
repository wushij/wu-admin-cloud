package com.wu.cloud.ai.service;

import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;

import java.util.Map;

public interface AiChatLogService {

    PageResult<AiChatLogDO> page(PageParam pageParam, String username, String provider,
                                 Integer chatStatus, String conversationId);

    boolean delete(Long id);

    boolean cleanLogs();

    void saveLogAsync(AiChatLogDO chatLog);

    /** Token 使用统计：总数/对话数/平均值/每日趋势 */
    Map<String, Object> tokenStats(String startDate, String endDate);
}
