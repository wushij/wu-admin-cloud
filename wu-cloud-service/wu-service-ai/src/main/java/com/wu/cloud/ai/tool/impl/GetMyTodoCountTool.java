package com.wu.cloud.ai.tool.impl;

import cn.hutool.json.JSONObject;
import com.wu.cloud.ai.tool.AiTool;
import com.wu.cloud.ai.tool.AiToolContext;
import com.wu.cloud.ai.tool.ToolAccessLevel;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 工具：查询「我的待办」数量（USER 级，仅限本人）
 * <p>待办数据属主为 wu-service-ticket（审批单/工单），经 OpenFeign 内网接口实时统计。
 * 身份取自服务端注入的 {@link AiToolContext}，严禁信任模型传入的用户标识。</p>
 */
@Component
public class GetMyTodoCountTool implements AiTool {

    @Resource
    private RemoteTicketService remoteTicketService;

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
        Long userId = ctx == null ? null : ctx.getUserId();
        if (userId == null || userId <= 0) {
            return new JSONObject().set("message", "未登录，无法查询待办").toString();
        }
        try {
            R<TicketDashboardDTO> resp = remoteTicketService.getDashboardStats(userId);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                TicketDashboardDTO stats = resp.getData();
                long pendingApproval = nvl(stats.getApprovalPendingCount());
                long pendingTicket = nvl(stats.getTicketOpenCount());
                return new JSONObject()
                        .set("pendingApprovalCount", pendingApproval)
                        .set("pendingTicketCount", pendingTicket)
                        .set("totalTodoCount", pendingApproval + pendingTicket)
                        .toString();
            }
            return new JSONObject().set("message", "待办统计服务暂不可用，请在工单与审批模块查看").toString();
        } catch (Exception e) {
            return new JSONObject().set("message", "待办统计服务调用失败，请在工单与审批模块查看").toString();
        }
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
