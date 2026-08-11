package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import org.springframework.stereotype.Component;

/**
 * 工具：查询近 N 天登录成功/失败次数（ADMIN 级，需 monitor:loginlog:list）
 * <p>登录日志归属 wu-service-system，AI 服务不直接持有该数据；此处返回友好占位。</p>
 */
@Component
public class GetLoginStatTool implements AiTool {

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
        return new JSONObject()
                .set("days", days)
                .set("message", "登录统计能力未接入，请在系统监控模块查看")
                .toString();
    }
}
