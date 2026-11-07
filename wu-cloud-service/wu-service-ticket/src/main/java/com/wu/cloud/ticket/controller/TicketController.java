package com.wu.cloud.ticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 工单接口（HTTP 方法/路径与既有规范对齐 + wu-admin-web）
 */
@Tag(name = "工单管理")
@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Resource
    private TicketService ticketService;

    @Operation(summary = "工单分页")
    @GetMapping("/page")
    public R<PageResult<TicketDO>> page(PageParam pageParam, String title, String status, String priority, Long assigneeUserId) {
        return R.ok(ticketService.page(pageParam, title, status, priority, assigneeUserId));
    }

    @Operation(summary = "工单详情")
    @GetMapping("/get")
    public R<TicketDO> get(@RequestParam Long id) {
        return R.ok(ticketService.getDetail(id));
    }

    @GetMapping("/{id}")
    public R<TicketDO> detail(@PathVariable Long id) {
        return R.ok(ticketService.getDetail(id));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "创建工单")
    @PostMapping("/create")
    public R<Long> create(@RequestBody TicketDO ticket) {
        return R.ok(ticketService.create(ticket));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "更新工单")
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
    @Operation(summary = "流转工单状态")
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
    @Operation(summary = "删除工单")
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
    @Operation(summary = "恢复工单")
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
    @Operation(summary = "彻底删除工单")
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

    @Operation(summary = "回收站分页")
    @GetMapping("/recycle/page")
    public R<PageResult<TicketDO>> recyclePage(PageParam pageParam, String title, String status, String priority) {
        return R.ok(ticketService.recyclePage(pageParam, title, status, priority));
    }

    @Log(title = "工单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "新增工单评论")
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

    @Operation(summary = "工单评论列表")
    @GetMapping("/comment/list")
    public R<List<TicketCommentDO>> listComments(@RequestParam Long ticketId) {
        return R.ok(ticketService.listComments(ticketId));
    }

    @Operation(summary = "工单处理人选项")
    @GetMapping("/assignee-options")
    public R<List<Map<String, Object>>> assigneeOptions() {
        return R.ok(ticketService.getAssigneeOptions());
    }

    @Operation(summary = "工单附件列表")
    @GetMapping("/attachment/list")
    public R<List<TicketAttachmentDO>> attachmentList(@RequestParam Long ticketId) {
        return R.ok(ticketService.listAttachments(ticketId));
    }

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/attachment/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Long> uploadAttachment(@RequestParam Long ticketId,
                                    @RequestPart("file") MultipartFile file) throws IOException {
        return R.ok(ticketService.uploadAttachment(ticketId, file));
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/attachment/download/{id}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) throws IOException {
        TicketAttachmentDO attachment = ticketService.getAttachmentForDownload(id);
        if (attachment == null) {
            return ResponseEntity.notFound().build();
        }
        // 文件服务存储的附件以 URL 记录，302 重定向到对象存储/静态资源地址
        String filePath = attachment.getFilePath();
        if (filePath != null && (filePath.startsWith("http://") || filePath.startsWith("https://"))) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(filePath))
                    .build();
        }
        // 历史本地磁盘附件（相对/绝对路径）按原逻辑读取
        Path localPath = Paths.get(filePath == null ? "" : filePath);
        if (filePath == null || !Files.exists(localPath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(localPath);
        String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}

