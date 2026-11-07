package com.wu.cloud.ticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.ticket.dto.ApprovalApproveReqVO;
import com.wu.cloud.ticket.dto.ApprovalArchiveReqVO;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.ApprovalRecordDO;
import com.wu.cloud.ticket.service.ApprovalFormService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批单接口（路径/方法与既有规范对齐 + wu-admin-web 契约）
 */
@Tag(name = "审批单中心")
@RestController
@RequestMapping("/api/approval")
public class ApprovalFormController {

    @Resource
    private ApprovalFormService approvalFormService;

    @Operation(summary = "审批单分页")
    @GetMapping("/page")
    public R<PageResult<ApprovalFormDO>> page(PageParam pageParam, String title, String formType, String status, Long applicantUserId) {
        return R.ok(approvalFormService.page(pageParam, title, formType, status, applicantUserId));
    }

    @Operation(summary = "审批单详情")
    @GetMapping("/get")
    public R<ApprovalFormDO> get(@RequestParam Long id) {
        return R.ok(approvalFormService.getDetail(id));
    }

    @GetMapping("/{id}")
    public R<ApprovalFormDO> detail(@PathVariable Long id) {
        return R.ok(approvalFormService.getDetail(id));
    }

    @Operation(summary = "提交审批单")
    @PostMapping("/create")
    public R<Long> create(@RequestBody ApprovalFormDO form) {
        return R.ok(approvalFormService.create(form));
    }

    @PostMapping("/update")
    public R<Boolean> update(@RequestBody ApprovalFormDO form) {
        approvalFormService.update(form);
        return R.ok(true);
    }

    /** 对齐前端 put('/approval/approve', { id, action, remark }) */
    @Operation(summary = "审批操作")
    @PutMapping("/approve")
    public R<Boolean> approve(@RequestBody ApprovalApproveReqVO req) {
        if (req == null || req.getId() == null) {
            return R.fail("审批单ID不能为空");
        }
        if ("REJECT".equalsIgnoreCase(req.getAction())) {
            approvalFormService.reject(req.getId(), req.getRemark());
        } else {
            approvalFormService.approve(req.getId(), req.getRemark());
        }
        return R.ok(true);
    }

    @PostMapping("/approve")
    public R<Boolean> approveByParam(@RequestParam Long id, @RequestParam(required = false) String remark) {
        approvalFormService.approve(id, remark);
        return R.ok(true);
    }

    @PostMapping("/reject")
    public R<Boolean> reject(@RequestParam Long id, @RequestParam(required = false) String remark) {
        approvalFormService.reject(id, remark);
        return R.ok(true);
    }

    /** 对齐前端 put('/approval/archive', { id, remark }) */
    @Operation(summary = "归档审批单")
    @PutMapping("/archive")
    public R<Boolean> archive(@RequestBody ApprovalArchiveReqVO req) {
        if (req == null || req.getId() == null) {
            return R.fail("审批单ID不能为空");
        }
        approvalFormService.archive(req.getId());
        return R.ok(true);
    }

    @PostMapping("/archive")
    public R<Boolean> archiveByParam(@RequestParam Long id) {
        approvalFormService.archive(id);
        return R.ok(true);
    }

    /** 对齐前端 del('/approval/delete', { params: { id } }) */
    @Operation(summary = "删除审批单")
    @DeleteMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        approvalFormService.delete(id);
        return R.ok(true);
    }

    @PostMapping("/delete")
    public R<Boolean> deleteByPost(@RequestParam Long id) {
        approvalFormService.delete(id);
        return R.ok(true);
    }

    @Operation(summary = "恢复审批单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam Long id) {
        approvalFormService.restore(id);
        return R.ok(true);
    }

    @PostMapping("/restore")
    public R<Boolean> restoreByPost(@RequestParam Long id) {
        approvalFormService.restore(id);
        return R.ok(true);
    }

    @Operation(summary = "彻底删除审批单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam Long id) {
        approvalFormService.deletePermanent(id);
        return R.ok(true);
    }

    @PostMapping("/delete-permanent")
    public R<Boolean> deletePermanentByPost(@RequestParam Long id) {
        approvalFormService.deletePermanent(id);
        return R.ok(true);
    }

    @Operation(summary = "审批单回收站分页")
    @GetMapping("/recycle/page")
    public R<PageResult<ApprovalFormDO>> recyclePage(PageParam pageParam, String title, String formType, String status) {
        return R.ok(approvalFormService.recyclePage(pageParam, title, formType, status));
    }

    @Operation(summary = "审批单记录")
    @GetMapping("/record/list")
    public R<List<ApprovalRecordDO>> recordList(@RequestParam Long formId) {
        return R.ok(approvalFormService.recordList(formId));
    }

    @Operation(summary = "审批人下拉（排除当前用户）")
    @GetMapping("/approver-options")
    public R<List<Map<String, Object>>> approverOptions() {
        return R.ok(approvalFormService.listApproverOptions());
    }

    @PostMapping("/withdraw")
    public R<Boolean> withdraw(@RequestParam Long id) {
        approvalFormService.withdraw(id);
        return R.ok(true);
    }

    @PostMapping("/submit-nodes")
    public R<Boolean> submitWithNodes(@RequestParam Long formId,
                                      @RequestParam List<Long> approverIds,
                                      @RequestParam List<String> nodeNames) {
        approvalFormService.submitWithNodes(formId, approverIds, nodeNames);
        return R.ok(true);
    }

    @PostMapping("/approve-node")
    public R<Boolean> approveNode(@RequestParam Long formId,
                                  @RequestParam Long nodeId,
                                  @RequestParam(required = false) String remark) {
        approvalFormService.approveNode(formId, nodeId, remark);
        return R.ok(true);
    }
}