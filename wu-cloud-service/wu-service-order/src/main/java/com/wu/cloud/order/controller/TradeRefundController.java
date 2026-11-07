package com.wu.cloud.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.order.entity.TradeRefundDO;
import com.wu.cloud.order.service.TradeRefundService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 退款单管理 Controller
 */
@Tag(name = "退款管理")
@RestController
@RequestMapping("/api/order/refund")
public class TradeRefundController {

    @Resource
    private TradeRefundService tradeRefundService;

    /** 退款单分页 */
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<TradeRefundDO>> page(PageParam pageParam,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String status) {
        return R.ok(tradeRefundService.page(pageParam, orderNo, status));
    }

    /** 退款单详情 */
    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<TradeRefundDO> detail(@PathVariable Long id) {
        return R.ok(tradeRefundService.detail(id));
    }

    /** 申请退款 */
    @Operation(summary = "新增")
    @PostMapping("/apply")
    public R<Long> apply(@RequestParam Long orderId, @RequestParam(required = false) String reason) {
        return R.ok(tradeRefundService.apply(orderId, reason));
    }

    /** 审核退款 */
    @Operation(summary = "审核")
    @PostMapping("/audit")
    public R<Boolean> audit(@RequestParam Long id,
                            @RequestParam Boolean approved,
                            @RequestParam(required = false) String remark) {
        tradeRefundService.audit(id, approved, remark);
        return R.ok(true);
    }
}
