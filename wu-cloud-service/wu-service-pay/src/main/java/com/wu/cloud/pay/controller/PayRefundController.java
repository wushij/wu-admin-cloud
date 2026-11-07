package com.wu.cloud.pay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.entity.PayRefundDO;
import com.wu.cloud.pay.service.PayRefundService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 退款单 Controller
 */
@Tag(name = "支付退款")
@RestController
@RequestMapping("/api/pay/refund")
public class PayRefundController {

    @Resource
    private PayRefundService payRefundService;

    /**
     * 分页查询退款单
     */
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<PayRefundDO>> page(PageParam pageParam,
                                           @RequestParam(required = false) String refundNo,
                                           @RequestParam(required = false) String orderNo,
                                           @RequestParam(required = false) String status) {
        return R.ok(payRefundService.page(pageParam, refundNo, orderNo, status));
    }

    /**
     * 退款单详情
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<PayRefundDO> detail(@PathVariable Long id) {
        return R.ok(payRefundService.detail(id));
    }

    /**
     * 申请退款
     */
    @Operation(summary = "新增")
    @PostMapping("/apply")
    public R<Long> apply(@RequestParam Long orderId,
                        @RequestParam(required = false) String reason) {
        return R.ok(payRefundService.apply(orderId, reason));
    }

    /**
     * 审核退款
     */
    @Operation(summary = "审核")
    @PostMapping("/audit")
    public R<Boolean> audit(@RequestParam Long id,
                            @RequestParam Boolean approved,
                            @RequestParam(required = false) String remark) {
        payRefundService.audit(id, approved, remark);
        return R.ok(true);
    }

    /**
     * 第三方退款回调（返回第三方要求的纯文本 SUCCESS / FAIL）
     */
    @Operation(summary = "批量已读")
    @PostMapping("/notify/refund")
    public String refundNotify(@RequestParam String refundNo,
                               @RequestParam(required = false) String transactionId,
                               @RequestParam(defaultValue = "true") boolean success) {
        try {
            payRefundService.handleRefundNotify(refundNo, transactionId, success);
            return "SUCCESS";
        } catch (Exception e) {
            return "FAIL";
        }
    }
}
