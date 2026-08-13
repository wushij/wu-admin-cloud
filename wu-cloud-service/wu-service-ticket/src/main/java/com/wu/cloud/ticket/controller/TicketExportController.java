package com.wu.cloud.ticket.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.TicketDO;
import com.wu.cloud.ticket.service.ApprovalFormService;
import com.wu.cloud.ticket.service.TicketService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单/审批导出（规范路径 /api/export/ticket|approval）
 */
@RestController
@RequestMapping("/api/export")
public class TicketExportController {

    private static final int MAX_ROWS = 5000;

    @Resource
    private TicketService ticketService;
    @Resource
    private ApprovalFormService approvalFormService;

    @RequiresPermissions("system:ticket:list")
    @GetMapping("/ticket")
    public void exportTicket(HttpServletResponse response,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) Long assigneeUserId) throws IOException {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(MAX_ROWS);
        PageResult<TicketDO> page = ticketService.page(pageParam, title, status, priority, assigneeUserId);
        List<TicketExportVO> rows = new ArrayList<>();
        if (page.getList() != null) {
            for (TicketDO t : page.getList()) {
                TicketExportVO vo = new TicketExportVO();
                vo.setTicketNo(t.getTicketNo());
                vo.setTitle(t.getTitle());
                vo.setPriority(t.getPriority());
                vo.setStatus(t.getStatus());
                vo.setCreatorName(t.getCreatorName());
                vo.setAssigneeName(t.getAssigneeName());
                vo.setDeadline(t.getDeadline());
                vo.setCreateTime(t.getCreateTime());
                rows.add(vo);
            }
        }
        writeExcel(response, "工单列表", TicketExportVO.class, rows);
    }

    @RequiresPermissions("system:approval:list")
    @GetMapping("/approval")
    public void exportApproval(HttpServletResponse response,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) String formType,
                               @RequestParam(required = false) String status) throws IOException {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(MAX_ROWS);
        PageResult<ApprovalFormDO> page = approvalFormService.page(pageParam, title, formType, status, null);
        List<ApprovalExportVO> rows = new ArrayList<>();
        if (page.getList() != null) {
            for (ApprovalFormDO a : page.getList()) {
                ApprovalExportVO vo = new ApprovalExportVO();
                vo.setFormNo(a.getFormNo());
                vo.setFormType(a.getFormType());
                vo.setTitle(a.getTitle());
                vo.setStatus(a.getStatus());
                vo.setApplicantName(a.getApplicantName());
                vo.setApproverName(a.getApproverName());
                vo.setCreateTime(a.getCreateTime());
                rows.add(vo);
            }
        }
        writeExcel(response, "审批单列表", ApprovalExportVO.class, rows);
    }

    private <T> void writeExcel(HttpServletResponse response, String sheetName,
                                Class<T> clazz, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(sheetName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(data);
    }

    @Data
    public static class TicketExportVO {
        @ExcelProperty("工单号")
        private String ticketNo;
        @ExcelProperty("标题")
        private String title;
        @ExcelProperty("优先级")
        private String priority;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("创建人")
        private String creatorName;
        @ExcelProperty("处理人")
        private String assigneeName;
        @ExcelProperty("截止时间")
        private LocalDateTime deadline;
        @ExcelProperty("创建时间")
        private LocalDateTime createTime;
    }

    @Data
    public static class ApprovalExportVO {
        @ExcelProperty("单号")
        private String formNo;
        @ExcelProperty("类型")
        private String formType;
        @ExcelProperty("标题")
        private String title;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("申请人")
        private String applicantName;
        @ExcelProperty("审批人")
        private String approverName;
        @ExcelProperty("创建时间")
        private LocalDateTime createTime;
    }
}
