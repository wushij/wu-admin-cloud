package com.wu.cloud.ai.service.impl;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.ai.dto.AiChatPageDTO;
import com.wu.cloud.ai.dto.AiChatStreamReqVO;
import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.entity.AiModelDO;
import com.wu.cloud.ai.mapper.AiChatLogMapper;
import com.wu.cloud.ai.providers.AiProviderFactory;
import com.wu.cloud.ai.providers.AiStreamListener;
import com.wu.cloud.ai.service.AiChatLogService;
import com.wu.cloud.ai.service.AiChatService;
import com.wu.cloud.ai.service.AiModelService;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.AiToolExecutor;
import com.wu.cloud.ai.tool.ToolInvocationResult;
import com.wu.cloud.ai.util.AiSanitizerUtil;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 单轮对话最长上下文条数与单条内容长度限制（成本防护） */
    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int MAX_CONTENT_LENGTH = 4000;
    /** 历史会话列表上限 */
    private static final int MAX_CONVERSATIONS = 30;
    /** 单会话恢复的最大问答轮数 */
    private static final int MAX_HISTORY_ROUNDS = 50;

    /** 流式对话专用线程池（SSE 长任务，不占用 Web 容器线程） */
    private final ExecutorService chatExecutor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNamePrefix("ai-chat-stream-").setDaemon(true).build());

    @Resource
    private AiModelService aiModelService;

    @Resource
    private AiChatLogService aiChatLogService;

    @Resource
    private AiProviderFactory aiProviderFactory;

    @Resource
    private PromptAssembler promptAssembler;

    @Resource
    private AiToolExecutor aiToolExecutor;

    @Resource
    private AiChatQuotaGuard aiChatQuotaGuard;

    @Resource
    private AiChatLogMapper aiChatLogMapper;

    @PreDestroy
    public void shutdown() {
        chatExecutor.shutdownNow();
    }

    @Override
    public SseEmitter stream(AiChatStreamReqVO reqVO, Long userId, String username) {
        // SSE 无超时（长回答场景），由客户端断开或流结束终止
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(e -> cancelled.set(true));

        List<AiChatStreamReqVO.Message> messages;
        AiModelDO model;
        try {
            messages = sanitizeMessages(reqVO.getMessages());
            model = aiModelService.getAvailableModel(reqVO.getModelId());
        } catch (BusinessException e) {
            sendErrorQuietly(emitter, e.getMessage());
            return emitter;
        }

        // 用户级限流与 token 配额（fail-open）：被拒经 SSE error 下发，不落 chat log（未产生上游成本）
        String quotaErr = aiChatQuotaGuard.check(userId);
        if (quotaErr != null) {
            log.warn("AI 对话被限流拒绝 user={}: {}", userId, quotaErr);
            sendErrorQuietly(emitter, quotaErr);
            return emitter;
        }

        // L1 知识注入：服务端组装 system 消息插入首位（sanitize 已丢弃客户端伪造的 system 角色）
        try {
            String latestUserQuery = extractLatestUserQuery(messages);
            String systemPrompt = promptAssembler.assemble(userId, model.getSystemPrompt(),
                    reqVO.getSource(), latestUserQuery);
            if (StrUtil.isNotBlank(systemPrompt)) {
                AiChatStreamReqVO.Message systemMsg = new AiChatStreamReqVO.Message();
                systemMsg.setRole("system");
                systemMsg.setContent(systemPrompt);
                messages.add(0, systemMsg);
            }
        } catch (Exception e) {
            log.warn("AI system 提示词组装失败，降级为内置人设: {}", e.getMessage());
        }

        String plainApiKey = aiModelService.decryptApiKey(model);
        long startTime = System.currentTimeMillis();
        StringBuilder answerBuffer = new StringBuilder();

        // L3 工具：按用户角色可见性过滤 tool 声明；身份上下文从会话注入，工具轨迹用于审计
        final String toolSpecs = aiToolExecutor.buildToolSpecs(userId);
        final AiToolContext toolCtx = new AiToolContext(userId, username);
        final JSONArray toolTrace = new JSONArray();

        chatExecutor.submit(() -> {
            AiStreamListener listener = new AiStreamListener() {
                @Override
                public void onDelta(String delta) {
                    answerBuffer.append(delta);
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException | IllegalStateException e) {
                        cancelled.set(true);
                    }
                }

                @Override
                public void onComplete(int promptTokens, int completionTokens) {
                    long duration = System.currentTimeMillis() - startTime;
                    boolean aborted = cancelled.get();
                    if (!aborted) {
                        try {
                            JSONObject done = new JSONObject()
                                    .set("promptTokens", promptTokens)
                                    .set("completionTokens", completionTokens)
                                    .set("totalTokens", promptTokens + completionTokens)
                                    .set("durationMs", duration);
                            emitter.send(SseEmitter.event().name("done").data(done.toString()));
                            emitter.complete();
                        } catch (IOException | IllegalStateException e) {
                            cancelled.set(true);
                        }
                    }
                    // token 用量回写配额计数（成功与中断均回写）
                    long tokensForQuota = resolveTokensForQuota(promptTokens, completionTokens, messages, answerBuffer.toString());
                    aiChatQuotaGuard.recordUsage(userId, tokensForQuota);
                    saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                            promptTokens, completionTokens, duration, cancelled.get() ? 2 : 1, null, toolTrace);
                }

                @Override
                public boolean isCancelled() {
                    return cancelled.get();
                }

                @Override
                public boolean toolsEnabled() {
                    return StrUtil.isNotBlank(toolSpecs);
                }

                @Override
                public String toolSpecsJson() {
                    return toolSpecs;
                }

                @Override
                public String executeTool(String toolName, String argsJson) {
                    ToolInvocationResult r = aiToolExecutor.invoke(toolName, argsJson, toolCtx);
                    toolTrace.add(new JSONObject()
                            .set("tool", toolName)
                            .set("denied", r.isDenied())
                            .set("durationMs", r.getDurationMs()));
                    return r.getResult();
                }

                @Override
                public void onToolStatus(String toolName) {
                    if (cancelled.get()) {
                        return;
                    }
                    try {
                        JSONObject status = new JSONObject()
                                .set("tool", toolName)
                                .set("message", toolStatusMessage(toolName));
                        emitter.send(SseEmitter.event().name("status").data(status.toString()));
                    } catch (IOException | IllegalStateException e) {
                        cancelled.set(true);
                    }
                }
            };

            try {
                aiProviderFactory.getStrategy(model).streamChat(model, plainApiKey, messages, listener);
            } catch (Exception e) {
                log.error("AI 流式对话失败 provider={} model={}: {}", model.getProvider(), model.getModelName(), e.getMessage());
                String errMsg = e instanceof BusinessException ? e.getMessage() : "AI 服务暂时不可用，请稍后再试";
                sendErrorQuietly(emitter, errMsg);
                saveChatLog(reqVO, model, userId, username, messages, answerBuffer.toString(),
                        0, 0, System.currentTimeMillis() - startTime, 0, StrUtil.brief(e.getMessage(), 480), toolTrace);
            }
        });
        return emitter;
    }

    /** 校验并脱敏消息上下文 */
    private List<AiChatStreamReqVO.Message> sanitizeMessages(List<AiChatStreamReqVO.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        List<AiChatStreamReqVO.Message> result = new ArrayList<>();
        int from = Math.max(0, messages.size() - MAX_CONTEXT_MESSAGES);
        for (int i = from; i < messages.size(); i++) {
            AiChatStreamReqVO.Message msg = messages.get(i);
            if (msg == null || StrUtil.isBlank(msg.getContent())) {
                continue;
            }
            String role = "assistant".equals(msg.getRole()) ? "assistant" : "user";
            AiChatStreamReqVO.Message clean = new AiChatStreamReqVO.Message();
            clean.setRole(role);
            clean.setContent(AiSanitizerUtil.sanitize(StrUtil.brief(msg.getContent(), MAX_CONTENT_LENGTH)));
            result.add(clean);
        }
        if (result.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        return result;
    }

    /** 取本轮最后一条用户提问（用于 L2 知识库检索） */
    private String extractLatestUserQuery(List<AiChatStreamReqVO.Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatStreamReqVO.Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }

    /** 计算用于配额计数的 token 数：供应商未返回 usage 时按字符数估算 */
    private long resolveTokensForQuota(int promptTokens, int completionTokens,
                                       List<AiChatStreamReqVO.Message> messages, String answer) {
        int total = promptTokens + completionTokens;
        if (total > 0) {
            return total;
        }
        long chars = 0;
        if (messages != null) {
            for (AiChatStreamReqVO.Message m : messages) {
                if (m != null && m.getContent() != null) {
                    chars += m.getContent().length();
                }
            }
        }
        if (answer != null) {
            chars += answer.length();
        }
        return chars <= 0 ? 0 : chars / 2;
    }

    /** 工具执行前的前端提示文案 */
    private String toolStatusMessage(String toolName) {
        if (toolName == null) {
            return "⚡ 正在查询系统数据...";
        }
        switch (toolName) {
            case "get_online_user_count":
                return "⚡ 正在查询在线用户数...";
            case "get_login_stat":
                return "⚡ 正在统计登录数据...";
            case "get_my_todo_count":
                return "⚡ 正在查询您的待办...";
            case "get_announce_latest":
                return "⚡ 正在获取最新公告...";
            default:
                return "⚡ 正在查询系统数据...";
        }
    }

    private void sendErrorQuietly(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(StrUtil.nullToEmpty(message)));
            emitter.complete();
        } catch (IOException | IllegalStateException ignored) {
            // 客户端已断开，忽略
        }
    }

    private void saveChatLog(AiChatStreamReqVO reqVO, AiModelDO model, Long userId, String username,
                             List<AiChatStreamReqVO.Message> messages, String answer,
                             int promptTokens, int completionTokens, long durationMs,
                             int chatStatus, String errorMsg, JSONArray toolTrace) {
        AiChatLogDO chatLog = new AiChatLogDO();
        chatLog.setUserId(userId);
        chatLog.setUsername(StrUtil.nullToEmpty(username));
        chatLog.setConversationId(StrUtil.nullToEmpty(reqVO.getConversationId()));
        chatLog.setModelId(model.getId());
        chatLog.setProvider(model.getProvider());
        chatLog.setModelName(model.getModelName());
        // 记录本轮最后一条用户提问（已脱敏）
        String question = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                question = messages.get(i).getContent();
                break;
            }
        }
        chatLog.setQuestion(question);
        chatLog.setAnswer(answer);
        chatLog.setPromptTokens(promptTokens);
        chatLog.setCompletionTokens(completionTokens);
        chatLog.setTotalTokens(promptTokens + completionTokens);
        chatLog.setDurationMs(durationMs);
        chatLog.setChatStatus(chatStatus);
        chatLog.setErrorMsg(errorMsg);
        chatLog.setSource("mobile".equalsIgnoreCase(reqVO.getSource()) ? "mobile" : "pc");
        chatLog.setToolTrace(toolTrace == null || toolTrace.isEmpty() ? null : toolTrace.toString());
        chatLog.setCreator(StrUtil.nullToEmpty(username));
        chatLog.setUpdater(StrUtil.nullToEmpty(username));
        aiChatLogService.saveLogAsync(chatLog);
    }

    // ------------------------------------------------------------------
    // 会话管理（原有能力）
    // ------------------------------------------------------------------

    @Override
    public Page<AiChatLogDO> getChatLogPage(AiChatPageDTO pageDTO) {
        Page<AiChatLogDO> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        LambdaQueryWrapper<AiChatLogDO> wrapper = new LambdaQueryWrapper<>();
        if (pageDTO.getQuestion() != null && !pageDTO.getQuestion().isBlank()) {
            wrapper.like(AiChatLogDO::getQuestion, pageDTO.getQuestion());
        }
        wrapper.orderByDesc(AiChatLogDO::getCreateTime);
        return aiChatLogMapper.selectPage(page, wrapper);
    }

    @Override
    public AiChatLogDO getChatLogById(Long id) {
        return aiChatLogMapper.selectById(id);
    }

    @Override
    public List<Map<String, Object>> listConversations() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return List.of();
        }
        return aiChatLogMapper.selectConversations(userId, MAX_CONVERSATIONS);
    }

    @Override
    public List<AiChatLogDO> listHistory(String conversationId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        // 严格限定本人会话，防止用任意 conversationId 探测他人对话
        return aiChatLogMapper.selectList(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId)
                .orderByAsc(AiChatLogDO::getId)
                .last("LIMIT " + MAX_HISTORY_ROUNDS));
    }

    @Override
    public boolean deleteConversation(String conversationId) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || conversationId == null || conversationId.isBlank()) {
            return false;
        }
        int rows = aiChatLogMapper.delete(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId));
        return rows > 0;
    }

    @Override
    public boolean renameConversation(String conversationId, String title) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null || conversationId == null || conversationId.isBlank() || title == null) {
            return false;
        }
        List<AiChatLogDO> list = aiChatLogMapper.selectList(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId)
                .eq(AiChatLogDO::getConversationId, conversationId)
                .orderByDesc(AiChatLogDO::getId)
                .last("LIMIT 1"));
        if (list.isEmpty()) {
            return false;
        }
        AiChatLogDO latest = list.get(0);
        latest.setQuestion(title);
        return aiChatLogMapper.updateById(latest) > 0;
    }

    @Override
    public boolean clearConversations() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return false;
        }
        aiChatLogMapper.delete(new LambdaQueryWrapper<AiChatLogDO>()
                .eq(AiChatLogDO::getUserId, userId));
        return true;
    }
}
