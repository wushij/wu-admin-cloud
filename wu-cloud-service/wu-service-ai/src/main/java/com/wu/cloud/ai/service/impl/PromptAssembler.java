package com.wu.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.system.dto.AiUserContextDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.ai.util.AiPromptTemplates;
import com.wu.cloud.common.config.DynamicConfigHelper;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L1 系统提示词组装器
 * <p>
 * 人设 → 全局知识 → 菜单结构 → 用户上下文(昵称/部门/角色名) → 模型 systemPrompt → 注入防护。
 * 用户档案/部门/角色/菜单统一由 wu-service-system 的 /inner/system/user/ai-context
 * 聚合接口提供（单次 RPC），本服务不持有任何用户侧表；
 * 组装结果按用户维度缓存 Redis 10 分钟，降低 RPC 压力。
 * </p>
 */
@Component
public class PromptAssembler {

    private static final Logger log = LoggerFactory.getLogger(PromptAssembler.class);

    private static final String CACHE_KEY_PREFIX = "ai:user_context:v2:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int CONTEXT_CHAR_BUDGET = 3000;
    private static final int USER_CTX_MAX_CHARS = 400;
    private static final int MAX_CHILDREN_PER_TOP = 12;

    @Resource
    private DynamicConfigHelper dynamicConfigHelper;

    @Resource
    private KnowledgeRetriever knowledgeRetriever;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RemoteUserService remoteUserService;

    public String assemble(Long userId, String modelSystemPrompt, String source, String latestUserQuery) {
        StringBuilder sb = new StringBuilder(AiPromptTemplates.PERSONA);
        if ("focus".equals(dynamicConfigHelper.getAiAnswerScope())) {
            sb.append(AiPromptTemplates.FOCUS_SCOPE_RULE);
        }

        JSONObject cached = loadUserBlocks(userId, source);
        String userCtx = cached.getStr("ctx", "");
        String menuFull = cached.getStr("menu", "");
        String menuTop = cached.getStr("menuTop", "");
        String knowledge = safeGlobalKnowledge();

        int budget = CONTEXT_CHAR_BUDGET;
        userCtx = StrUtil.brief(userCtx, USER_CTX_MAX_CHARS);
        budget -= userCtx.length();

        if (knowledge.length() > budget) {
            log.warn("AI 全局知识块超出容量预算被截断: {} -> {} chars", knowledge.length(), budget);
            knowledge = StrUtil.brief(knowledge, Math.max(budget, 0));
        }
        budget -= knowledge.length();

        String menu = menuFull;
        if (menu.length() > budget) {
            menu = menuTop.length() <= budget ? menuTop : "";
            log.warn("AI 菜单知识超出剩余预算，已降级为{}（full={} chars, budget={}）",
                    menu.isEmpty() ? "舍弃" : "仅顶级", menuFull.length(), budget);
        }

        if (StrUtil.isNotBlank(knowledge)) {
            sb.append("\n<project_knowledge>\n").append(knowledge).append("\n</project_knowledge>\n");
        }
        if (StrUtil.isNotBlank(menu)) {
            sb.append("\n<menu_structure>\n当前用户在系统中可见的功能菜单（回答功能类问题以此为准，不要编造其它模块）：\n")
                    .append(menu).append("\n</menu_structure>\n");
        }
        if (StrUtil.isNotBlank(userCtx)) {
            sb.append("\n<user_context>\n").append(userCtx).append("\n</user_context>\n");
        } else {
            log.warn("AI 用户上下文为空 userId={}，身份类问题将无法准确回答", userId);
            sb.append("\n<user_context>\n当前无法确认登录用户的账号/角色/部门，回答身份类问题时必须明确说明「无法获取」，禁止编造角色或部门。\n</user_context>\n");
        }

        String referenceBlock = safeRetrieve(latestUserQuery);
        if (StrUtil.isNotBlank(referenceBlock)) {
            sb.append('\n').append(referenceBlock).append('\n');
        }
        if (StrUtil.isNotBlank(modelSystemPrompt)) {
            sb.append('\n').append(modelSystemPrompt.trim()).append('\n');
        }
        sb.append(AiPromptTemplates.INJECTION_GUARD);
        return sb.toString();
    }

    public String assemble(Long userId, String modelSystemPrompt, String latestUserQuery) {
        return assemble(userId, modelSystemPrompt, "pc", latestUserQuery);
    }

    private String safeRetrieve(String latestUserQuery) {
        if (StrUtil.isBlank(latestUserQuery)) {
            return "";
        }
        try {
            return knowledgeRetriever.retrieveReferenceBlock(latestUserQuery);
        } catch (Exception e) {
            log.warn("AI 知识库检索异常，本轮跳过知识注入: {}", e.getMessage());
            return "";
        }
    }

    private JSONObject loadUserBlocks(Long userId, String source) {
        if (userId == null || userId <= 0) {
            return new JSONObject();
        }
        String cacheKey = CACHE_KEY_PREFIX + userId;
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cachedJson)) {
                JSONObject json = JSONUtil.parseObj(cachedJson);
                // 空上下文缓存作废，强制重建
                String ctx = json.getStr("ctx", "");
                if (StrUtil.isNotBlank(ctx) && ctx.contains("当前用户：")) {
                    json.set("ctx", appendSource(ctx, source));
                    return json;
                }
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文缓存读取失败，降级实时构建: {}", e.getMessage());
        }

        AiUserContextDTO aiContext = fetchAiContext(userId);
        String ctx = aiContext == null ? "" : buildUserContext(aiContext);
        List<MenuRow> menus = aiContext == null ? List.of() : toMenuRows(aiContext.getMenus());
        String menu = buildMenuKnowledge(menus);
        String menuTop = buildTopLevelOnly(menus);
        JSONObject blocks = new JSONObject()
                .set("ctx", ctx)
                .set("menu", menu)
                .set("menuTop", menuTop);

        // 仅在成功拿到用户档案时写入缓存，避免把空上下文锁死 10 分钟
        if (StrUtil.isNotBlank(ctx) && ctx.contains("当前用户：")) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, blocks.toString(), CACHE_TTL);
            } catch (Exception e) {
                log.warn("AI 用户上下文缓存写入失败（不影响本轮对话）: {}", e.getMessage());
            }
        } else {
            try {
                stringRedisTemplate.delete(cacheKey);
            } catch (Exception ignored) {
                // ignore
            }
        }

        JSONObject result = JSONUtil.parseObj(blocks.toString());
        result.set("ctx", appendSource(result.getStr("ctx", ""), source));
        return result;
    }

    /**
     * 用户上下文聚合 RPC：一次调用拿齐档案/部门/角色/可见菜单
     */
    private AiUserContextDTO fetchAiContext(Long userId) {
        try {
            R<AiUserContextDTO> resp = remoteUserService.getAiContext(userId);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                return resp.getData();
            }
            log.warn("AI 用户上下文 RPC 未返回有效数据 userId={}", userId);
        } catch (Exception e) {
            log.warn("AI 用户上下文 RPC 调用失败 userId={}: {}", userId, e.getMessage());
        }
        return null;
    }

    /** 构建用户上下文（昵称/账号/部门/角色） */
    private String buildUserContext(AiUserContextDTO ctx) {
        List<String> lines = new ArrayList<>();
        String username = StrUtil.nullToEmpty(ctx.getUsername());
        String nickname = StrUtil.blankToDefault(ctx.getNickname(), username);
        if (StrUtil.isNotBlank(nickname)) {
            lines.add("当前用户：" + nickname + " (" + username + ")");
        }
        if (StrUtil.isNotBlank(ctx.getDeptName())) {
            lines.add("所属部门：" + ctx.getDeptName());
        }
        if (CollUtil.isNotEmpty(ctx.getRoleNames())) {
            lines.add("角色：" + String.join("、", ctx.getRoleNames()));
        }
        if (!lines.isEmpty()) {
            log.info("AI 用户上下文已注入 userId={} lines={}", ctx.getUserId(), lines.size());
        }
        return String.join("\n", lines);
    }

    private List<MenuRow> toMenuRows(List<SysMenuDTO> menus) {
        if (CollUtil.isEmpty(menus)) {
            return List.of();
        }
        List<MenuRow> rows = new ArrayList<>(menus.size());
        for (SysMenuDTO menu : menus) {
            if (menu != null && StrUtil.isNotBlank(menu.getName())) {
                rows.add(new MenuRow(menu.getId(), menu.getName(), menu.getParentId()));
            }
        }
        return rows;
    }

    private String buildMenuKnowledge(List<MenuRow> menus) {
        if (menus.isEmpty()) {
            return "";
        }
        Map<Long, List<String>> childrenByParent = new LinkedHashMap<>();
        List<MenuRow> topLevels = new ArrayList<>();
        for (MenuRow menu : menus) {
            long parentId = menu.parentId == null ? 0L : menu.parentId;
            if (parentId == 0L) {
                topLevels.add(menu);
            } else {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menu.name);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (MenuRow top : topLevels) {
            sb.append("- ").append(top.name);
            List<String> children = childrenByParent.get(top.id);
            if (CollUtil.isNotEmpty(children)) {
                List<String> limited = children.size() > MAX_CHILDREN_PER_TOP
                        ? children.subList(0, MAX_CHILDREN_PER_TOP)
                        : children;
                sb.append("：").append(String.join("、", limited));
                if (children.size() > MAX_CHILDREN_PER_TOP) {
                    sb.append(" 等");
                }
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private String buildTopLevelOnly(List<MenuRow> menus) {
        if (menus.isEmpty()) {
            return "";
        }
        List<String> tops = new ArrayList<>();
        for (MenuRow menu : menus) {
            long parentId = menu.parentId == null ? 0L : menu.parentId;
            if (parentId == 0L && StrUtil.isNotBlank(menu.name)) {
                tops.add(menu.name);
            }
        }
        return tops.isEmpty() ? "" : "- " + String.join("、", tops);
    }

    private String appendSource(String ctx, String source) {
        String sourceLine = "mobile".equalsIgnoreCase(source) ? "当前使用移动端" : "当前使用 PC 端";
        return StrUtil.isBlank(ctx) ? sourceLine : ctx + "\n" + sourceLine;
    }

    private String safeGlobalKnowledge() {
        try {
            return StrUtil.trim(dynamicConfigHelper.getAiGlobalKnowledge());
        } catch (Exception e) {
            log.warn("AI 全局知识读取失败，本轮跳过注入: {}", e.getMessage());
            return "";
        }
    }

    private record MenuRow(Long id, String name, Long parentId) {
    }
}
