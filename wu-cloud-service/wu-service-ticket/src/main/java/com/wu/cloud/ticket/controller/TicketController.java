package com.wu.cloud.ticket.controller;

import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.ticket.dto.TicketCommentCreateReqVO;
import com.wu.cloud.ticket.dto.TicketTransitionReqVO;
import com.wu.cloud.ticket.entity.TicketAttachmentDO;
import com.wu.cloud.ticket.entity.TicketCommentDO;
import com.wu.cloud.ticket.entity.TicketDO;
import com.wu.cloud.ticket.service.TicketService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 工单接口（HTTP 方法/路径对齐单体 + wu-admin-web）
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    @GetMapping("/page")
    public R<PageResult<TicketDO>> page(PageParam pageParam, String title, String status, String priority, Long assigneeUserId) {
        return R.ok(ticketService.page(pageParam, title, status, priority, assigneeUserId));
    }

    @GetMapping("/get")
    public R<TicketDO> get(@RequestParam Long id) {
        return R.ok(ticketService.getDetail(id));
    }

    @GetMapping("/{id}")
    public R<TicketDO> detail(@PathVariable Long id) {
        return R.ok(ticketService.getDetail(id));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Long> create(@RequestBody TicketDO ticket) {
        return R.ok(ticketService.create(ticket));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody TicketDO ticket) {
        ticketService.update(ticket);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Boolean> updateByPost(@RequestBody TicketDO ticket) {
        ticketService.update(ticket);
        return R.ok(true);
    }

    /** 对齐前端 put('/ticket/transition', { id, status }) */
    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/transition")
    public R<Boolean> transition(@RequestBody TicketTransitionReqVO req) {
        if (req == null || req.getId() == null) {
            return R.fail("工单ID不能为空");
        }
        ticketService.changeStatus(req.getId(), req.getStatus());
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/status")
    public R<Boolean> changeStatus(@RequestParam Long id, @RequestParam String status) {
        ticketService.changeStatus(id, status);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/assign")
    public R<Boolean> assign(@RequestParam Long id, @RequestParam Long assigneeUserId) {
        ticketService.assign(id, assigneeUserId);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        ticketService.delete(id);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @PostMapping("/delete")
    public R<Boolean> deleteByPost(@RequestParam Long id) {
        ticketService.delete(id);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @PostMapping("/delete-batch")
    public R<Boolean> deleteBatch(@RequestParam Long[] ids) {
        ticketService.deleteBatch(ids);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam Long id) {
        ticketService.restore(id);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/restore")
    public R<Boolean> restoreByPost(@RequestParam Long id) {
        ticketService.restore(id);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam Long id) {
        ticketService.deletePermanent(id);
        return R.ok(true);
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.DELETE)
    @PostMapping("/delete-permanent")
    public R<Boolean> deletePermanentByPost(@RequestParam Long id) {
        ticketService.deletePermanent(id);
        return R.ok(true);
    }

    @GetMapping("/recycle/page")
    public R<PageResult<TicketDO>> recyclePage(PageParam pageParam, String title, String status, String priority) {
        return R.ok(ticketService.recyclePage(pageParam, title, status, priority));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/comment/create")
    public R<Long> createComment(@RequestBody TicketCommentCreateReqVO req) {
        if (req == null || req.getTicketId() == null) {
            return R.fail("工单ID不能为空");
        }
        return R.ok(ticketService.addComment(req.getTicketId(), req.getContent()));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @PostMapping("/comment")
    public R<Long> addComment(@RequestParam Long ticketId, @RequestParam String content) {
        return R.ok(ticketService.addComment(ticketId, content));
    }

    @GetMapping("/comment/list")
    public R<List<TicketCommentDO>> listComments(@RequestParam Long ticketId) {
        return R.ok(ticketService.listComments(ticketId));
    }

    @GetMapping("/assignee-options")
    public R<List<Map<String, Object>>> assigneeOptions() {
        return R.ok(ticketService.getAssigneeOptions());
    }

    @GetMapping("/attachment/list")
    public R<List<TicketAttachmentDO>> attachmentList(@RequestParam Long ticketId) {
        return R.ok(ticketService.listAttachments(ticketId));
    }

    @PostMapping(value = "/attachment/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Long> uploadAttachment(@RequestParam Long ticketId,
                                    @RequestPart("file") MultipartFile file) throws IOException {
        return R.ok(ticketService.uploadAttachment(ticketId, file));
    }

    @GetMapping("/attachment/download/{id}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) throws IOException {
        TicketAttachmentDO attachment = ticketService.getAttachmentForDownload(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        Path filePath = Paths.get(attachment.getFilePath());
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(filePath);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}

