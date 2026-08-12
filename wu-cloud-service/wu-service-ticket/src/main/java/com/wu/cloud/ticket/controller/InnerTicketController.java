package com.wu.cloud.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.ticket.dto.TicketDTO;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.entity.TicketDO;
import com.wu.cloud.ticket.mapper.ApprovalFormMapper;
import com.wu.cloud.ticket.mapper.NoticeMapper;
import com.wu.cloud.ticket.mapper.TicketMapper;
import com.wu.cloud.ticket.service.TicketService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务内部 RPC 端点。
 * <p>契约对齐：返回值统一使用 {@code TicketDTO}，与 wu-cloud-api 中
 * {@code RemoteTicketService} 声明一致。</p>
 */
@RestController
@RequestMapping("/inner/ticket")
public class InnerTicketController {

    @Resource
    private TicketService ticketService;
    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private NoticeMapper noticeMapper;

    @GetMapping("/{id}")
    public R<TicketDTO> getTicketById(@PathVariable("id") Long id) {
        return R.ok(toDTO(ticketService.getById(id)));
    }

    /**
     * 消息中心拉取某用户未读通知数（缺实现曾导致 inboxCount 恒 0）
     */
    @GetMapping("/notice/unread-count/{userId}")
    public R<Long> getNoticeUnreadCount(@PathVariable("userId") Long userId) {
        if (userId == null) {
            return R.ok(0L);
        }
        Long count = noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
        return R.ok(count == null ? 0L : count);
    }

    /**
     * 工作台工单/审批统计（对齐单体：按当前用户「待我处理/待我审批」过滤）
     */
    @GetMapping("/dashboard-stats")
    public R<TicketDashboardDTO> getDashboardStats(@RequestParam(value = "userId", required = false) Long userId) {
        TicketDashboardDTO dto = new TicketDashboardDTO();
        if (userId == null || userId <= 0) {
            dto.setTicketOpenCount(0L);
            dto.setTicketOverdueCount(0L);
            dto.setApprovalPendingCount(0L);
            return R.ok(dto);
        }
        // 待我处理：OPEN 且指派给我，或尚未指派
        Long open = ticketMapper.selectCount(new LambdaQueryWrapper<TicketDO>()
                .eq(TicketDO::getStatus, "OPEN")
                .and(w -> w.eq(TicketDO::getAssigneeUserId, userId)
                        .or().eq(TicketDO::getAssigneeUserId, 0)
                        .or().isNull(TicketDO::getAssigneeUserId)));
        dto.setTicketOpenCount(open == null ? 0L : open);
        // 逾期：OPEN/IN_PROGRESS 且超过截止时间，且指派给我或未指派
        List<String> overdueStatus = List.of("OPEN", "IN_PROGRESS");
        Long overdue = ticketMapper.selectCount(new LambdaQueryWrapper<TicketDO>()
                .in(TicketDO::getStatus, overdueStatus)
                .isNotNull(TicketDO::getDeadline)
                .lt(TicketDO::getDeadline, LocalDateTime.now())
                .and(w -> w.eq(TicketDO::getAssigneeUserId, userId)
                        .or().eq(TicketDO::getAssigneeUserId, 0)
                        .or().isNull(TicketDO::getAssigneeUserId)));
        dto.setTicketOverdueCount(overdue == null ? 0L : overdue);
        // 待我审批
        Long pending = approvalFormMapper.selectCount(new LambdaQueryWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getStatus, "SUBMITTED")
                .eq(ApprovalFormDO::getApproverUserId, userId));
        dto.setApprovalPendingCount(pending == null ? 0L : pending);
        return R.ok(dto);
    }

    private TicketDTO toDTO(TicketDO ticket) {
        if (ticket == null) {
            return null;
        }
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setTicketNo(ticket.getTicketNo());
        dto.setTitle(ticket.getTitle());
        dto.setDescription(ticket.getDescription());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());
        dto.setCreatorUserId(ticket.getCreatorUserId());
        dto.setAssigneeUserId(ticket.getAssigneeUserId());
        dto.setCreateTime(ticket.getCreateTime());
        return dto;
    }
}
