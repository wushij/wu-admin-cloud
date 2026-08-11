package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 工具：查询当前在线用户数（ADMIN 级，需 monitor:online:list）
 */
@Component
public class GetOnlineUserCountTool implements AiTool {

    @Resource
    private RemoteOnlineUserService remoteOnlineUserService;

    @Override
    public String name() {
        return "get_online_user_count";
    }

    @Override
    public String description() {
        return "查询当前系统的在线用户数量。当用户询问「现在有多少人在线」「在线人数」等实时数据时调用。";
    }

    @Override
    public JSONObject parametersSchema() {
        return new JSONObject().set("type", "object").set("properties", new JSONObject());
    }

    @Override
    public ToolAccessLevel accessLevel() {
        return ToolAccessLevel.ADMIN;
    }

    @Override
    public String requiredPermission() {
        return "monitor:online:list";
    }

    @Override
    public String execute(JSONObject args, AiToolContext ctx) {
        int count = 0;
        try {
            R<Integer> res = remoteOnlineUserService.countOnlineUsers();
            if (res != null && res.isSuccess() && res.getData() != null) {
                count = res.getData();
            }
        } catch (Exception ignored) {
        }
        return new JSONObject().set("onlineUserCount", count).toString();
    }
}
