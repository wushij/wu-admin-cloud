package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.api.message.feign.RemoteMessageService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工具：查询最新公告标题列表（USER 级，仅已发布公告）
 * <p>公告数据属主为 wu-service-message，经 OpenFeign 内网接口实时获取。</p>
 */
@Component
public class GetAnnounceLatestTool implements AiTool {

    private static final int MAX_LIMIT = 5;

    @Resource
    private RemoteMessageService remoteMessageService;

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
        try {
            R<List<SysAnnounceDTO>> resp = remoteMessageService.listLatestAnnounces(MAX_LIMIT);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                JSONArray announcements = new JSONArray();
                for (SysAnnounceDTO announce : resp.getData()) {
                    if (announce == null) {
                        continue;
                    }
                    announcements.add(new JSONObject()
                            .set("title", announce.getTitle())
                            .set("time", announce.getCreateTime() == null ? "" : announce.getCreateTime().toString()));
                }
                return new JSONObject().set("announcements", announcements).toString();
            }
            return new JSONObject().set("announcements", new JSONArray())
                    .set("message", "公告服务暂不可用，请在消息模块查看")
                    .toString();
        } catch (Exception e) {
            return new JSONObject().set("announcements", new JSONArray())
                    .set("message", "公告服务调用失败，请在消息模块查看")
                    .toString();
        }
    }
}
