package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import com.wu.cloud.api.system.feign.RemoteStatsService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具：查询近 N 天登录成功/失败次数（ADMIN 级，需 monitor:loginlog:list）
 * <p>登录日志属主为 wu-service-system，经 OpenFeign 内网接口实时统计。</p>
 */
@Component
public class GetLoginStatTool implements AiTool {

    @Resource
    private RemoteStatsService remoteStatsService;

    @Override
    public String name() {
        return "get_login_stat";
    }

    @Override
    public String description() {
        return "统计近 N 天的登录成功与失败次数。当用户询问登录统计、近期登录情况、登录失败次数等问题时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        JSONObject days = new JSONObject()
                .set("type", "integer")
                .set("description", "统计的天数，默认 7，范围 1~30");
        return new JSONObject()
                .set("type", "object")
                .set("properties", new JSONObject().set("days", days));
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.ADMIN;
    }

    @Override
    public String requiredPermission() {
        return "monitor:loginlog:list";
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        int days = args == null ? 7 : args.getInt("days", 7);
        if (days < 1) {
            days = 1;
        } else if (days > 30) {
            days = 30;
        }
        try {
            R<Map<String, Long>> resp = remoteStatsService.countLoginByStartTime(days);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                long success = resp.getData().getOrDefault("successCount", 0L);
                long fail = resp.getData().getOrDefault("failCount", 0L);
                return new JSONObject()
                        .set("days", days)
                        .set("successCount", success)
                        .set("failCount", fail)
                        .toString();
            }
            return new JSONObject()
                    .set("days", days)
                    .set("message", "登录统计服务暂不可用，请稍后在系统监控模块查看")
                    .toString();
        } catch (Exception e) {
            return new JSONObject()
                    .set("days", days)
                    .set("message", "登录统计服务调用失败，请稍后在系统监控模块查看")
                    .toString();
        }
    }
}
