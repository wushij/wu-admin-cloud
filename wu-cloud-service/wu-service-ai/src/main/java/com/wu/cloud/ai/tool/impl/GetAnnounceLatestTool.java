package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import org.springframework.stereotype.Component;

/**
 * 工具：查询最新公告标题列表（USER 级，仅已发布公告）
 * <p>公告数据归属 wu-service-message，AI 服务不直接持有；此处返回友好占位。</p>
 */
@Component
public class GetAnnounceLatestTool implements AiTool {

    @Override
    public String name() {
        return "get_announce_latest";
    }

    @Override
    public String description() {
        return "查询系统最新发布的公告标题列表。当用户询问「最新公告」「有什么通知」等问题时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        return new JSONObject().set("type", "object").set("properties", new JSONObject());
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.USER;
    }

    @Override
    public String requiredPermission() {
        return null;
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        return new JSONObject().set("announcements", new JSONArray())
                .set("message", "公告获取能力未接入，请在消息模块查看")
                .toString();
    }
}
