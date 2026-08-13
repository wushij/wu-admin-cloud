package com.wu.cloud.order.controller;

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
@RestController
@RequestMapping("/api/order/refund")
public class TradeRefundController {

    @Resource
    private TradeRefundService tradeRefundService;

    /** 退款单分页 */
    @GetMapping("/page")
    public R<PageResult<TradeRefundDO>> page(PageParam pageParam,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String status) {
        return R.ok(tradeRefundService.page(pageParam, orderNo, status));
    }

    /** 退款单详情 */
    @GetMapping("/{id}")
    public R<TradeRefundDO> detail(@PathVariable Long id) {
        return R.ok(tradeRefundService.detail(id));
    }

    /** 申请退款 */
    @PostMapping("/apply")
    public R<Long> apply(@RequestParam Long orderId, @RequestParam(required = false) String reason) {
        return R.ok(tradeRefundService.apply(orderId, reason));
    }

    /** 审核退款 */
    @PostMapping("/audit")
    public R<Boolean> audit(@RequestParam Long id,
                            @RequestParam Boolean approved,
                            @RequestParam(required = false) String remark) {
        tradeRefundService.audit(id, approved, remark);
        return R.ok(true);
    }
}
