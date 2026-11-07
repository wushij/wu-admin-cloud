package com.wu.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.mapper.AiChatLogMapper;
import com.wu.cloud.ai.service.AiChatLogService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话日志 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，直接注入 Mapper）
 */
@Slf4j
@Service
public class AiChatLogServiceImpl implements AiChatLogService {

    @Resource
    private AiChatLogMapper aiChatLogMapper;

    @Override
    public PageResult<AiChatLogDO> page(PageParam pageParam, String username, String provider,
                                        Integer chatStatus, String conversationId) {
        Page<AiChatLogDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<AiChatLogDO> wrapper = new LambdaQueryWrapper<AiChatLogDO>()
                .like(username != null && !username.isBlank(), AiChatLogDO::getUsername, username)
                .eq(provider != null && !provider.isBlank(), AiChatLogDO::getProvider, provider)
                .eq(chatStatus != null, AiChatLogDO::getChatStatus, chatStatus)
                .eq(conversationId != null && !conversationId.isBlank(), AiChatLogDO::getConversationId, conversationId)
                .orderByDesc(AiChatLogDO::getId);
        Page<AiChatLogDO> result = aiChatLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public boolean delete(Long id) {
        if (aiChatLogMapper.selectById(id) == null) {
            throw new BusinessException(404, "对话日志不存在");
        }
        return aiChatLogMapper.deleteById(id) > 0;
    }

    @Override
    public boolean cleanLogs() {
        return aiChatLogMapper.delete(new LambdaQueryWrapper<>()) > 0;
    }

    @Async
    @Override
    public void saveLogAsync(AiChatLogDO chatLog) {
        try {
            aiChatLogMapper.insert(chatLog);
        } catch (Exception e) {
            log.error("AI 对话日志异步落库失败: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> tokenStats(String startDate, String endDate) {
        List<Map<String, Object>> daily = aiChatLogMapper.selectDailyTokenStats(startDate, endDate);
        long totalTokens = 0L;
        long totalChats = 0L;
        for (Map<String, Object> row : daily) {
            long tokens = toLong(row.get("tokens"));
            long chats = toLong(row.get("chats"));
            totalTokens += tokens;
            totalChats += chats;
            row.put("tokens", tokens);
            row.put("chats", chats);
        }
        long avgTokens = totalChats > 0 ? totalTokens / totalChats : 0L;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTokens", totalTokens);
        result.put("totalChats", totalChats);
        result.put("avgTokens", avgTokens);
        result.put("dailyTrend", daily);
        return result;
    }

    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
