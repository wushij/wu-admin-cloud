package com.wu.cloud.api.ticket.feign;

import com.wu.cloud.api.ticket.dto.TicketDTO;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.fallback.RemoteTicketServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程工单服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteTicketService", value = "wu-service-ticket", path = "/inner/ticket", fallbackFactory = RemoteTicketServiceFallback.class)
public interface RemoteTicketService {

    @GetMapping("/{id}")
    R<TicketDTO> getTicketById(@PathVariable("id") Long id);

    @GetMapping("/notice/unread-count/{userId}")
    R<Long> getNoticeUnreadCount(@PathVariable("userId") Long userId);

    /** 工作台「待我」工单/审批统计（按 assignee/approver 过滤） */
    @GetMapping("/dashboard-stats")
    R<TicketDashboardDTO> getDashboardStats(@RequestParam(value = "userId", required = false) Long userId);

    /** 清理回收站中超过保留期的工单/审批/通知（物理删除），返回清理条数 */
    @PostMapping("/recycle/purge")
    R<Integer> purgeRecycleBin(@RequestParam(value = "retentionDays", defaultValue = "30") Integer retentionDays);
}
