package com.wu.cloud.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.pay.entity.PayOrderDO;
import java.math.BigDecimal;
import com.wu.cloud.pay.entity.PayRefundDO;
import com.wu.cloud.pay.mapper.PayOrderMapper;
import com.wu.cloud.pay.mapper.PayRefundMapper;
import com.wu.cloud.pay.service.PayRefundService;
import com.wu.cloud.pay.service.PayService;
import com.wu.cloud.pay.service.PayServiceFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 退款单 Service 实现
 */
@Slf4j
@Service
public class PayRefundServiceImpl extends ServiceImpl<PayRefundMapper, PayRefundDO> implements PayRefundService {

    @Resource
    private PayOrderMapper payOrderMapper;
    @Resource
    private PayServiceFactory payServiceFactory;

    @Override
    public PageResult<PayRefundDO> page(PageParam pageParam, String refundNo, String orderNo, String status) {
        Page<PayRefundDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<PayRefundDO> wrapper = new LambdaQueryWrapper<>();
        if (refundNo != null && !refundNo.isBlank()) {
            wrapper.like(PayRefundDO::getRefundNo, refundNo);
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.eq(PayRefundDO::getOrderNo, orderNo);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(PayRefundDO::getStatus, status);
        }
        wrapper.orderByDesc(PayRefundDO::getCreateTime);
        Page<PayRefundDO> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public PayRefundDO detail(Long id) {
        PayRefundDO refund = getById(id);
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在");
        }
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long orderId, String reason) {
        PayOrderDO order = payOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "原支付订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可申请退款");
        }
        PayRefundDO refund = new PayRefundDO();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(order.getOrderNo());
        refund.setPayType(order.getPayType());
        refund.setAmount(order.getAmount());
        refund.setStatus("PENDING");
        refund.setReason(reason);
        refund.setApplicantUserId(UserContextHolder.getUserId());
        refund.setApplicantName(UserContextHolder.getUsername());
        save(refund);
        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRefundDO applyByOrderNo(String orderNo, BigDecimal amount, String reason) {
        PayOrderDO order = payOrderMapper.selectOne(
                new LambdaQueryWrapper<PayOrderDO>().eq(PayOrderDO::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException("支付订单不存在: " + orderNo);
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可退款: " + orderNo);
        }
        PayRefundDO refund = new PayRefundDO();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderNo(order.getOrderNo());
        refund.setPayType(order.getPayType());
        refund.setAmount(amount != null ? amount : order.getAmount());
        refund.setStatus("APPROVED");
        refund.setReason(reason);
        refund.setAuditRemark("order 服务内部自动退款");
        save(refund);
        // 联动第三方渠道执行真实退款
        execute(refund.getId());
        return getById(refund.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Boolean approved, String remark) {
        PayRefundDO refund = getById(id);
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在");
        }
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException("仅待审核退款单可审核");
        }
        refund.setAuditorUserId(UserContextHolder.getUserId());
        refund.setAuditorName(UserContextHolder.getUsername());
        refund.setAuditRemark(remark);
        if (Boolean.TRUE.equals(approved)) {
            refund.setStatus("APPROVED");
            updateById(refund);
            // 审核通过后自动执行退款
            execute(id);
        } else {
            refund.setStatus("REJECTED");
            updateById(refund);
        }
    }

    /**
     * 执行退款：调用第三方退款，状态 PROCESSING → SUCCESS。
     * <p>真实渠道异步回调时由 {@code handleRefundNotify} 更新最终状态。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long id) {
        PayRefundDO refund = getById(id);
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在");
        }
        if (!"APPROVED".equals(refund.getStatus())) {
            throw new BusinessException("仅审核通过的退款单可执行");
        }
        // 调用第三方退款（微信 PROCESSING 异步 / 支付宝 SUCCESS 同步）
        PayService payService = payServiceFactory.getService(refund.getPayType());
        Map<String, String> result = payService.refund(refund.getOrderNo(), refund.getRefundNo(), refund.getAmount());

        // 记录第三方退款单号
        String txnId = result.get("refundId");
        if (txnId == null || txnId.isBlank()) {
            txnId = result.get("tradeNo");
        }
        if (txnId != null && !txnId.isBlank()) {
            refund.setTransactionId(txnId);
        }

        String refundStatus = result.get("refundStatus");
        // 同步退款成功（支付宝）直接终态；异步（微信 PROCESSING）等待回调更新
        if ("SUCCESS".equalsIgnoreCase(refundStatus)) {
            refund.setStatus("SUCCESS");
            refund.setRefundTime(LocalDateTime.now());
        } else {
            refund.setStatus("PROCESSING");
        }
        updateById(refund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundNotify(String refundNo, String transactionId, boolean success) {
        PayRefundDO refund = getOne(new LambdaQueryWrapper<PayRefundDO>()
                .eq(PayRefundDO::getRefundNo, refundNo));
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在: " + refundNo);
        }
        // 幂等：已终态直接返回
        if ("SUCCESS".equals(refund.getStatus()) || "FAILED".equals(refund.getStatus())) {
            return;
        }
        if (transactionId != null && !transactionId.isBlank()) {
            refund.setTransactionId(transactionId);
        }
        if (success) {
            refund.setStatus("SUCCESS");
            refund.setRefundTime(LocalDateTime.now());
        } else {
            refund.setStatus("FAILED");
        }
        updateById(refund);
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (System.nanoTime() % 1000);
    }
}
