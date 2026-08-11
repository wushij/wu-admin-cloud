package com.wu.cloud.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.mapper.TradeOrderMapper;
import com.wu.cloud.order.service.TradeOrderService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * <p>
 * 每 5 分钟扫描一次，将 PENDING 状态且创建时间超过 30 分钟的订单自动取消并归还库存。
 * </p>
 */
@Component
public class OrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);

    /** 超时阈值（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private TradeOrderService tradeOrderService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<TradeOrderDO> timeoutOrders = tradeOrderMapper.selectList(
                new LambdaQueryWrapper<TradeOrderDO>()
                        .eq(TradeOrderDO::getStatus, "PENDING")
                        .lt(TradeOrderDO::getCreateTime, deadline));
        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }
        log.info("订单超时取消任务：扫描到 {} 笔超时待支付订单", timeoutOrders.size());
        for (TradeOrderDO order : timeoutOrders) {
            try {
                // cancel 内部会校验状态并归还库存
                tradeOrderService.cancel(order.getId());
                log.info("订单超时取消成功：orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("订单超时取消失败：orderId={}, err={}", order.getId(), e.getMessage(), e);
            }
        }
    }
}
