package com.wu.cloud.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/order")
public class TradeOrderController {

    @Resource
    private TradeOrderService tradeOrderService;

    @Resource
    private StockService stockService;

    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<TradeOrderDO>> page(PageParam pageParam, String orderNo, Long userId, String status) {
        return R.ok(tradeOrderService.page(pageParam, orderNo, userId, status));
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<TradeOrderDO> detail(@PathVariable Long id) {
        return R.ok(tradeOrderService.getDetail(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> create(@RequestBody TradeOrderDO order) {
        return R.ok(tradeOrderService.create(order));
    }

    @Operation(summary = "修改")
    @PostMapping("/update")
    public R<Boolean> update(@RequestBody TradeOrderDO order) {
        tradeOrderService.update(order);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/pay")
    public R<Boolean> pay(@RequestParam Long id, @RequestParam String payType) {
        tradeOrderService.pay(id, payType);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/ship")
    public R<Boolean> ship(@RequestParam Long id) {
        tradeOrderService.ship(id);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/complete")
    public R<Boolean> complete(@RequestParam Long id) {
        tradeOrderService.complete(id);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/cancel")
    public R<Boolean> cancel(@RequestParam Long id) {
        tradeOrderService.cancel(id);
        return R.ok(true);
    }

    @Operation(summary = "删除")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        tradeOrderService.delete(id);
        return R.ok(true);
    }

    @Operation(summary = "在线用户列表")
    @GetMapping("/user/list")
    public R<List<TradeOrderDO>> listByUser(@RequestParam Long userId) {
        return R.ok(tradeOrderService.listByUser(userId));
    }

    /** 退款：仅 PAID/SHIPPED/COMPLETED 可退款 */
    @Operation(summary = "新增")
    @PostMapping("/refund")
    public R<Boolean> refund(@RequestParam Long id) {
        tradeOrderService.refund(id);
        return R.ok(true);
    }

    /** 通用状态流转 */
    @Operation(summary = "新增")
    @PostMapping("/transition")
    public R<Boolean> transition(@RequestParam Long id, @RequestParam String status) {
        tradeOrderService.transition(id, status);
        return R.ok(true);
    }

    /** 库存列表 */
    @Operation(summary = "在线用户列表")
    @GetMapping("/stock/list")
    public R<List<StockDO>> stockList() {
        return R.ok(stockService.listStock());
    }

    /** 订单统计分析 */
    @Operation(summary = "Redis 实时统计（图表）")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        return R.ok(tradeOrderService.stats(startDate, endDate));
    }
}
