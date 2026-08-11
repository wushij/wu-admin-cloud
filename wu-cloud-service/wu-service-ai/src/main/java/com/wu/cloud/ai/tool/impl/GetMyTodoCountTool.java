package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import org.springframework.stereotype.Component;

/**
 * 工具：查询「我的待办」数量（USER 级，仅限本人）
 * <p>待办数据归属 wu-service-ticket（审批单/工单），AI 服务不直接持有；此处返回友好占位。</p>
 */
@Component
public class GetMyTodoCountTool implements AiTool {

    @Override
    public String name() {
        return "get_my_todo_count";
    }

    @Override
    public String description() {
        return "查询当前登录用户的待办数量，包括待我审批的审批单与指派给我的未完结工单。当用户询问「我有多少待办」「我要审批什么」等问题时调用。";
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
        return new JSONObject()
                .set("message", "待办统计能力未接入，请在工单与审批模块查看")
                .toString();
    }
}
