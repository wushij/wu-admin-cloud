package com.wu.cloud.ai.tool;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.common.core.context.UserContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * L3 工具执行器（双重闸门 + 超时熔断）
 * <p>
 * 第一道闸门-可见性过滤：{@link #buildToolSpecs} 按用户角色/权限裁剪 tools 声明；
 * 第二道闸门-执行前二次鉴权：{@link #invoke} 以会话 userId 重新校验（不信任可见性过滤结果）。
 * 权限/角色取自线程上下文 {@link UserContextHolder}（由网关注入），微服务下无需跨服务查询。
 * 所有执行统一 CompletableFuture 硬超时熔断。
 * </p>
 */
@Component
public class AiToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiToolExecutor.class);

    @Resource
    private List<AiTool> tools;

    /** 工具执行硬超时（毫秒） */
    @Value("${ai.tool-timeout-ms:3000}")
    private long toolTimeoutMs;

    private final Map<String, AiTool> toolByName = new LinkedHashMap<>();

    /** 工具执行专用线程池（隔离 Web 容器线程，配合硬超时熔断） */
    private final ExecutorService toolExecutor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNamePrefix("ai-tool-").setDaemon(true).build());

    @PostConstruct
    public void init() {
        for (AiTool tool : tools) {
            toolByName.put(tool.name(), tool);
        }
        log.info("AI 工具注册完成，共 {} 个: {}", toolByName.size(), toolByName.keySet());
    }

    @PreDestroy
    public void shutdown() {
        toolExecutor.shutdownNow();
    }

    /** 构建 OpenAI tools 声明数组 JSON（可见性过滤后），无可用工具返回 null */
    public String buildToolSpecs(Long userId) {
        List<AiTool> visible = visibleTools(userId);
        if (visible.isEmpty()) {
            return null;
        }
        JSONArray arr = new JSONArray();
        for (AiTool tool : visible) {
            JSONObject function = new JSONObject()
                    .set("name", tool.name())
                    .set("description", tool.description())
                    .set("parameters", tool.parametersSchema());
            arr.add(new JSONObject().set("type", "function").set("function", function));
        }
        return arr.toString();
    }

    /** 执行工具：第二道闸门鉴权 + 硬超时熔断；任何失败都返回可回传模型的结果文本，绝不抛出 */
    public ToolInvocationResult invoke(String toolName, String argsJson, AiToolContext ctx) {
        long start = System.currentTimeMillis();
        AiTool tool = toolByName.get(toolName);
        if (tool == null) {
            log.warn("AI 工具调用被拒绝：未注册工具 {}", toolName);
            return denied(toolName, "该工具不存在", start);
        }
        if (!isAuthorized(tool, ctx.getUserId())) {
            log.warn("AI 工具越权尝试：user={} tool={} 无权限 {}", ctx.getUserId(), toolName, tool.requiredPermission());
            return denied(toolName, "无权查询该数据", start);
        }
        JSONObject args = parseArgs(argsJson);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> tool.execute(args, ctx), toolExecutor);
        try {
            String result = future.get(toolTimeoutMs, TimeUnit.MILLISECONDS);
            return new ToolInvocationResult(toolName, StrUtil.nullToEmpty(result), false, System.currentTimeMillis() - start);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("AI 工具执行超时({}ms)：{}", toolTimeoutMs, toolName);
            return denied(toolName, "{\"error\":\"查询超时，请稍后再试\"}", start);
        } catch (Exception e) {
            future.cancel(true);
            log.error("AI 工具执行异常：{} - {}", toolName, e.getMessage());
            return denied(toolName, "{\"error\":\"查询失败\"}", start);
        }
    }

    // ------------------------------------------------------------------

    /** 可见工具：USER 级全部可见；ADMIN 级需超管角色或对应权限 */
    private List<AiTool> visibleTools(Long userId) {
        List<AiTool> result = new ArrayList<>();
        if (userId == null || userId <= 0) {
            return result;
        }
        for (AiTool tool : tools) {
            if (isAuthorized(tool, userId)) {
                result.add(tool);
            }
        }
        return result;
    }

    private boolean isAuthorized(AiTool tool, Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        if (tool.accessLevel() == ToolAccessLevel.USER) {
            return true;
        }
        // ADMIN 级：超管角色或具备对应权限（取自线程上下文，由网关注入）
        try {
            if (hasRole("super_admin")) {
                return true;
            }
            String perm = tool.requiredPermission();
            return StrUtil.isNotBlank(perm) && hasPermission(perm);
        } catch (Exception e) {
            log.warn("AI 工具鉴权异常，按无权限处理: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasRole(String role) {
        return UserContextHolder.getRoles() != null && UserContextHolder.getRoles().contains(role);
    }

    private boolean hasPermission(String permission) {
        return UserContextHolder.getPermissions() != null && UserContextHolder.getPermissions().contains(permission);
    }

    private JSONObject parseArgs(String argsJson) {
        if (StrUtil.isBlank(argsJson)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(argsJson);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private ToolInvocationResult denied(String toolName, String message, long start) {
        return new ToolInvocationResult(toolName, message, true, System.currentTimeMillis() - start);
    }
}
