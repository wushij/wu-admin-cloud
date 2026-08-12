package com.wu.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.ai.util.AiPromptTemplates;
import com.wu.cloud.common.config.DynamicConfigHelper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * L1 系统提示词组装器（对齐单体 PromptAssembler）
 * <p>
 * 人设 → 全局知识 → 菜单结构 → 用户上下文(昵称/部门/角色名) → 模型 systemPrompt → 注入防护。
 * AI 与 system 共用同一业务库，直接 JDBC 查询（与单体同库直读一致），避免 Feign 失败导致空上下文。
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
    private JdbcTemplate jdbcTemplate;

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
                // 旧版空缓存（只有端来源）作废，强制重建
                String ctx = json.getStr("ctx", "");
                if (StrUtil.isNotBlank(ctx) && ctx.contains("当前用户：")) {
                    json.set("ctx", appendSource(ctx, source));
                    return json;
                }
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文缓存读取失败，降级实时构建: {}", e.getMessage());
        }

        String ctx = buildUserContext(userId);
        String menu = buildMenuKnowledge(userId);
        String menuTop = buildTopLevelOnly(userId);
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

    /** 构建用户上下文（昵称/账号/角色名/部门），对齐单体 SystemConfigHelper 侧的 PromptAssembler */
    private String buildUserContext(Long userId) {
        List<String> lines = new ArrayList<>();
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT username, nickname, dept_id FROM sys_user WHERE id = ? AND deleted = 0 LIMIT 1",
                    userId);
            if (!users.isEmpty()) {
                Map<String, Object> user = users.get(0);
                String username = str(user.get("username"));
                String nickname = StrUtil.blankToDefault(str(user.get("nickname")), username);
                lines.add("当前用户：" + nickname + " (" + username + ")");
                Long deptId = toLong(user.get("dept_id"));
                String deptName = resolveDeptName(deptId);
                if (StrUtil.isNotBlank(deptName)) {
                    lines.add("所属部门：" + deptName);
                }
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文-用户信息构建失败 userId={}: {}", userId, e.getMessage());
        }
        try {
            String roleNames = resolveRoleNames(userId);
            if (StrUtil.isNotBlank(roleNames)) {
                lines.add("角色：" + roleNames);
            }
        } catch (Exception e) {
            log.warn("AI 用户上下文-角色信息构建失败 userId={}: {}", userId, e.getMessage());
        }
        if (!lines.isEmpty()) {
            log.info("AI 用户上下文已注入 userId={} lines={}", userId, lines.size());
        }
        return String.join("\n", lines);
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null || deptId <= 0) {
            return "";
        }
        try {
            List<String> names = jdbcTemplate.query(
                    "SELECT name FROM sys_dept WHERE id = ? AND deleted = 0 LIMIT 1",
                    (rs, i) -> rs.getString(1), deptId);
            return CollUtil.isEmpty(names) ? "" : StrUtil.nullToEmpty(names.get(0));
        } catch (Exception e) {
            return "";
        }
    }

    private String resolveRoleNames(Long userId) {
        List<String> names = jdbcTemplate.query(
                """
                        SELECT r.name FROM sys_role r
                        INNER JOIN sys_user_role ur ON ur.role_id = r.id
                        WHERE ur.user_id = ? AND r.deleted = 0
                        ORDER BY r.sort ASC, r.id ASC
                        """,
                (rs, i) -> rs.getString(1), userId);
        if (CollUtil.isEmpty(names)) {
            return "";
        }
        List<String> cleaned = new ArrayList<>();
        for (String name : names) {
            if (StrUtil.isNotBlank(name)) {
                cleaned.add(name.trim());
            }
        }
        return String.join("、", cleaned);
    }

    private String buildMenuKnowledge(Long userId) {
        List<MenuRow> menus = loadVisibleMenus(userId);
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

    private String buildTopLevelOnly(Long userId) {
        List<MenuRow> menus = loadVisibleMenus(userId);
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

    private List<MenuRow> loadVisibleMenus(Long userId) {
        try {
            boolean superAdmin = isSuperAdmin(userId);
            String sql = superAdmin
                    ? """
                    SELECT id, name, parent_id FROM sys_menu
                    WHERE deleted = 0 AND status = 1 AND type IN (1, 2)
                    ORDER BY sort ASC, id ASC
                    """
                    : """
                    SELECT DISTINCT m.id, m.name, m.parent_id
                    FROM sys_menu m
                    INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
                    INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
                    WHERE ur.user_id = ? AND m.deleted = 0 AND m.status = 1 AND m.type IN (1, 2)
                    ORDER BY m.sort ASC, m.id ASC
                    """;
            List<Map<String, Object>> rows = superAdmin
                    ? jdbcTemplate.queryForList(sql)
                    : jdbcTemplate.queryForList(sql, userId);
            List<MenuRow> menus = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                String name = str(row.get("name"));
                if (StrUtil.isBlank(name)) {
                    continue;
                }
                menus.add(new MenuRow(toLong(row.get("id")), name, toLong(row.get("parent_id"))));
            }
            return menus;
        } catch (Exception e) {
            log.warn("AI 菜单知识加载失败 userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private boolean isSuperAdmin(Long userId) {
        try {
            List<String> codes = jdbcTemplate.query(
                    """
                            SELECT r.code FROM sys_role r
                            INNER JOIN sys_user_role ur ON ur.role_id = r.id
                            WHERE ur.user_id = ? AND r.deleted = 0
                            """,
                    (rs, i) -> rs.getString(1), userId);
            if (CollUtil.isEmpty(codes)) {
                return false;
            }
            Set<String> permissions = null;
            try {
                permissions = com.wu.cloud.common.core.context.UserContextHolder.getPermissions();
            } catch (Exception ignored) {
                // ignore
            }
            if (permissions != null && permissions.contains("*:*:*")) {
                return true;
            }
            for (String code : codes) {
                if (code == null) {
                    continue;
                }
                String c = code.trim().toLowerCase();
                if ("admin".equals(c) || "super_admin".equals(c) || "superadmin".equals(c)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
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

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record MenuRow(Long id, String name, Long parentId) {
    }
}
