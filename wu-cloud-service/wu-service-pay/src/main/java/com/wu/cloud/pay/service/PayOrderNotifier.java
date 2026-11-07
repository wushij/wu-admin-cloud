package com.wu.cloud.pay.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.api.order.feign.RemoteOrderService;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.entity.PayOutboxMessageDO;
import com.wu.cloud.pay.mapper.PayOutboxMessageMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 支付成功 → 订单服务的最终一致性通知器（标准本地事务消息表模式）。
 *
 * <p><b>一致性模型</b>：
 * {@link PayTxnService#markPaidAndEnqueue} 在同一本地事务内完成
 * 「支付单置为 PAID + 写入 pay_outbox_message(PENDING)」，事务提交即保证消息必然存在；
 * 本类只负责<b>事务提交后</b>的投递加速：</p>
 * <ol>
 *   <li>提交后优先同步 Feign 调用 order 的 {@code /inner/order/notify-paid}（幂等），
 *       成功则把本地消息标记 SUCCESS，链路最短；</li>
 *   <li>同步失败（order 宕机/网络抖动/降级）时保持 PENDING，
 *       由 {@code PayOutboxCompensateJob} 以指数退避重试，最多 5 次后标记 DEAD 供人工介入。</li>
 * </ol>
 *
 * <p><b>不影响主流程</b>：所有异常都被捕获，不会让第三方回调收到失败响应
 * 而触发渠道侧无意义的重复回调（本地入账已完成，重复回调会被幂等拦截）。</p>
 */
@Slf4j
@Component
public class PayOrderNotifier {

    @Resource
    private RemoteOrderService remoteOrderService;
    @Resource
    private PayOutboxMessageMapper payOutboxMessageMapper;

    /**
     * 通知订单服务「该业务订单已支付」。若当前存在未提交事务，投递动作延迟到提交后执行。
     */
    public void notifyOrderPaid(String orderNo, String payType) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliver(orderNo, payType);
                }
            });
        } else {
            deliver(orderNo, payType);
        }
    }

    /**
     * 同步投递；成功则把本地消息标记 SUCCESS，失败留给补偿任务重试
     */
    private void deliver(String orderNo, String payType) {
        try {
            R<Boolean> resp = remoteOrderService.markPaidByOrderNo(orderNo, payType);
            if (resp != null && resp.isSuccess()) {
                markSuccess(orderNo);
                log.info("[Pay->Order] 同步通知成功: orderNo={}, matched={}", orderNo, resp.getData());
                return;
            }
            log.warn("[Pay->Order] 同步通知未成功，等待本地消息表补偿重试: orderNo={}, resp={}",
                    orderNo, resp == null ? "null" : resp.getMsg());
        } catch (Exception e) {
            log.warn("[Pay->Order] 同步通知异常，等待本地消息表补偿重试: orderNo={}, err={}",
                    orderNo, e.getMessage());
        }
    }

    private void markSuccess(String orderNo) {
        try {
            payOutboxMessageMapper.update(null, new LambdaUpdateWrapper<PayOutboxMessageDO>()
                    .eq(PayOutboxMessageDO::getBizKey, "pay:order:paid:" + orderNo)
                    .eq(PayOutboxMessageDO::getStatus, PayOutboxMessageDO.STATUS_PENDING)
                    .set(PayOutboxMessageDO::getStatus, PayOutboxMessageDO.STATUS_SUCCESS)
                    .set(PayOutboxMessageDO::getUpdateTime, java.time.LocalDateTime.now()));
        } catch (Exception e) {
            // 标记失败不影响正确性：补偿任务重试时对端幂等，重复通知无副作用
            log.warn("[Pay->Order] 本地消息标记 SUCCESS 失败（补偿任务会重试）: orderNo={}, err={}",
                    orderNo, e.getMessage());
        }
    }
}
