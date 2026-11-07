package com.wu.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.pay.dto.PayRefundDTO;
import com.wu.cloud.api.pay.feign.RemotePayService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.entity.TradeRefundDO;
import com.wu.cloud.order.mapper.TradeRefundMapper;
import com.wu.cloud.order.service.TradeOrderService;
import com.wu.cloud.order.service.TradeRefundService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 退款单 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，直接注入 Mapper）
 */
@Service
public class TradeRefundServiceImpl implements TradeRefundService {

    @Resource
    private TradeRefundMapper tradeRefundMapper;

    @Resource
    private TradeOrderService tradeOrderService;

    @Resource
    private RemotePayService remotePayService;

    @Override
    public PageResult<TradeRefundDO> page(PageParam pageParam, String orderNo, String status) {
        Page<TradeRefundDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<TradeRefundDO> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.eq(TradeRefundDO::getOrderNo, orderNo);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(TradeRefundDO::getStatus, status);
        }
        wrapper.orderByDesc(TradeRefundDO::getCreateTime);
        Page<TradeRefundDO> result = tradeRefundMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public TradeRefundDO detail(Long id) {
        TradeRefundDO refund = tradeRefundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在");
        }
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long orderId, String reason) {
        TradeOrderDO order = tradeOrderService.getDetail(orderId);
        String status = order.getStatus();
        // 仅已支付/已发货/已完成订单可申请退款
        if (!"PAID".equals(status) && !"SHIPPED".equals(status) && !"COMPLETED".equals(status)) {
            throw new BusinessException("当前订单状态不允许申请退款");
        }
        TradeRefundDO refund = new TradeRefundDO();
        refund.setRefundNo(generateRefundNo());
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setAmount(order.getPayAmount() != null ? order.getPayAmount() : order.getTotalAmount());
        refund.setReason(reason);
        refund.setStatus("PENDING");
        tradeRefundMapper.insert(refund);
        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Boolean approved, String remark) {
        TradeRefundDO refund = tradeRefundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException(404, "退款单不存在");
        }
        if (!"PENDING".equals(refund.getStatus())) {
            throw new BusinessException("退款单已审核，不可重复审核");
        }
        Long auditorUserId = UserContextHolder.getUserId();
        refund.setAuditorUserId(auditorUserId);
        refund.setAuditRemark(remark);

        if (approved == null || !approved) {
            // 审核驳回
            refund.setStatus("REJECTED");
            tradeRefundMapper.updateById(refund);
            return;
        }

        // 审核通过，调用支付服务发起真实退款
        refund.setStatus("APPROVED");
        tradeRefundMapper.updateById(refund);
        try {
            // 通过 Feign 调用支付服务发起退款（pay 服务创建退款单并联动第三方渠道）
            R<PayRefundDTO> payResult = remotePayService.applyRefund(
                    refund.getOrderNo(), refund.getAmount(), remark != null ? remark : refund.getReason());
            if (payResult == null || !payResult.isSuccess() || payResult.getData() == null) {
                refund.setStatus("FAILED");
                refund.setAuditRemark(remark != null ? remark : "支付服务退款受理失败");
                tradeRefundMapper.updateById(refund);
                throw new BusinessException("调用支付服务退款失败：" + (payResult == null ? "支付服务不可用" : payResult.getMsg()));
            }
            PayRefundDTO payRefund = payResult.getData();
            // 记录第三方退款单号并同步退款状态
            refund.setTransactionId(payRefund.getTransactionId());
            if ("SUCCESS".equals(payRefund.getStatus())) {
                refund.setStatus("SUCCESS");
                refund.setRefundTime(LocalDateTime.now());
            } else {
                refund.setStatus("PROCESSING");
            }
            tradeRefundMapper.updateById(refund);
            // 更新订单状态为 REFUNDED（由订单状态机校验合法性）
            tradeOrderService.refund(refund.getOrderId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            refund.setStatus("FAILED");
            refund.setAuditRemark(remark != null ? remark : "退款异常: " + e.getMessage());
            tradeRefundMapper.updateById(refund);
            throw new BusinessException("调用支付服务退款异常: " + e.getMessage());
        }
    }

    private String generateRefundNo() {
        return "R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (System.nanoTime() % 1000);
    }
}
