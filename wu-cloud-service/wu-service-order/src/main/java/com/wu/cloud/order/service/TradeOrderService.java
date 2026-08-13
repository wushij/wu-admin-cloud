package com.wu.cloud.order.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.order.entity.TradeOrderDO;

import java.util.List;
import java.util.Map;

/**
 * 交易订单 Service
 */
public interface TradeOrderService {

    PageResult<TradeOrderDO> page(PageParam pageParam, String orderNo, Long userId, String status);

    TradeOrderDO getDetail(Long id);

    Long create(TradeOrderDO order);

    void update(TradeOrderDO order);

    void pay(Long id, String payType);

    /**
     * 按业务订单号将订单置为已支付（幂等，供 pay 服务支付回调反向通知）。
     *
     * @return true 表示订单存在且被标记为已支付；false 表示订单不存在（可能是仅支付、无业务订单的场景）
     */
    boolean markPaidByOrderNo(String orderNo, String payType);

    void ship(Long id);

    void complete(Long id);

    void cancel(Long id);

    void delete(Long id);

    List<TradeOrderDO> listByUser(Long userId);

    /** 退款（仅 PAID/SHIPPED/COMPLETED 可退款） */
    void refund(Long id);

    /** 通用状态流转（校验状态流转矩阵） */
    void transition(Long id, String targetStatus);

    /** 订单统计分析 */
    Map<String, Object> stats(String startDate, String endDate);
}
