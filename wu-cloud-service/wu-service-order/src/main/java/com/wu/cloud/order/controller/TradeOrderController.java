package com.wu.cloud.order.controller;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.order.entity.StockDO;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.service.StockService;
import com.wu.cloud.order.service.TradeOrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class TradeOrderController {

    @Resource
    private TradeOrderService tradeOrderService;

    @Resource
    private StockService stockService;

    @GetMapping("/page")
    public R<PageResult<TradeOrderDO>> page(PageParam pageParam, String orderNo, Long userId, String status) {
        return R.ok(tradeOrderService.page(pageParam, orderNo, userId, status));
    }

    @GetMapping("/{id}")
    public R<TradeOrderDO> detail(@PathVariable Long id) {
        return R.ok(tradeOrderService.getDetail(id));
    }

    @PostMapping("/create")
    public R<Long> create(@RequestBody TradeOrderDO order) {
        return R.ok(tradeOrderService.create(order));
    }

    @PostMapping("/update")
    public R<Boolean> update(@RequestBody TradeOrderDO order) {
        tradeOrderService.update(order);
        return R.ok(true);
    }

    @PostMapping("/pay")
    public R<Boolean> pay(@RequestParam Long id, @RequestParam String payType) {
        tradeOrderService.pay(id, payType);
        return R.ok(true);
    }

    @PostMapping("/ship")
    public R<Boolean> ship(@RequestParam Long id) {
        tradeOrderService.ship(id);
        return R.ok(true);
    }

    @PostMapping("/complete")
    public R<Boolean> complete(@RequestParam Long id) {
        tradeOrderService.complete(id);
        return R.ok(true);
    }

    @PostMapping("/cancel")
    public R<Boolean> cancel(@RequestParam Long id) {
        tradeOrderService.cancel(id);
        return R.ok(true);
    }

    @PostMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        tradeOrderService.delete(id);
        return R.ok(true);
    }

    @GetMapping("/user/list")
    public R<List<TradeOrderDO>> listByUser(@RequestParam Long userId) {
        return R.ok(tradeOrderService.listByUser(userId));
    }

    /** 退款：仅 PAID/SHIPPED/COMPLETED 可退款 */
    @PostMapping("/refund")
    public R<Boolean> refund(@RequestParam Long id) {
        tradeOrderService.refund(id);
        return R.ok(true);
    }

    /** 通用状态流转 */
    @PostMapping("/transition")
    public R<Boolean> transition(@RequestParam Long id, @RequestParam String status) {
        tradeOrderService.transition(id, status);
        return R.ok(true);
    }

    /** 库存列表 */
    @GetMapping("/stock/list")
    public R<List<StockDO>> stockList() {
        return R.ok(stockService.listStock());
    }

    /** 订单统计分析 */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        return R.ok(tradeOrderService.stats(startDate, endDate));
    }
}
