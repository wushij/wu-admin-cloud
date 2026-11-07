package com.wu.cloud.pay.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.entity.PayOutboxMessageDO;
import com.wu.cloud.pay.mapper.PayOrderMapper;
import com.wu.cloud.pay.mapper.PayOutboxMessageMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付入账本地事务服务。
 * <p>
 * 「支付单状态流转为 PAID」与「写入本地消息表」在同一个数据库事务内完成
 * （标准 Transactional Outbox），两者要么同时成功、要么同时回滚，
 * 彻底消除「已入账但无通知记录」的掉单窗口。
 * 独立成 Bean 是为了保证事务注解经代理生效（避免同类自调用导致事务失效）。
 * </p>
 */
@Slf4j
@Service
public class PayTxnService {

    @Resource
    private PayOrderMapper payOrderMapper;
    @Resource
    private PayOutboxMessageMapper payOutboxMessageMapper;

    /**
     * 条件更新支付单为已支付，并在同一事务内写入待投递的本地消息。
     *
     * @return true 表示本次真正完成入账（并已登记通知消息）；false 表示已入账/订单不存在，幂等跳过
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaidAndEnqueue(String payType, String orderNo, String transactionId) {
        PayOrderDO order = payOrderMapper.selectOne(new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            log.error("[Pay] 回调订单不存在: orderNo={}, payType={}", orderNo, payType);
            return false;
        }
        if (PayOrderService.STATUS_PAID.equals(order.getStatus())) {
            log.info("[Pay] 订单已入账，幂等跳过: orderNo={}", orderNo);
            return false;
        }
        // 条件更新：只有仍处于 PENDING 的订单才会被更新，影响行数即为幂等判据
        int rows = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderNo, orderNo)
                .eq(PayOrderDO::getStatus, PayOrderService.STATUS_PENDING)
                .set(PayOrderDO::getStatus, PayOrderService.STATUS_PAID)
                .set(PayOrderDO::getTransactionId, transactionId)
                .set(PayOrderDO::getPaidTime, LocalDateTime.now()));
        if (rows == 0) {
            log.info("[Pay] 并发入账已被其他线程完成: orderNo={}", orderNo);
            return false;
        }
        log.info("[Pay] 入账成功: orderNo={}, transactionId={}", orderNo, transactionId);

        // 同一事务内登记本地消息（Outbox）：事务提交即保证消息必然存在
        PayOutboxMessageDO message = new PayOutboxMessageDO();
        message.setBizKey("pay:order:paid:" + orderNo);
        message.setOrderNo(orderNo);
        message.setPayType(payType);
        message.setStatus(PayOutboxMessageDO.STATUS_PENDING);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        message.setCreateTime(LocalDateTime.now());
        payOutboxMessageMapper.insert(message);
        return true;
    }
}
