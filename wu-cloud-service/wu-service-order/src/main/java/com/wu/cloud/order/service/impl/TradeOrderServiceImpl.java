package com.wu.cloud.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.order.entity.OrderItemDO;
import com.wu.cloud.order.entity.TradeOrderDO;
import com.wu.cloud.order.mapper.OrderItemMapper;
import com.wu.cloud.order.mapper.TradeOrderMapper;
import com.wu.cloud.order.service.StockService;
import com.wu.cloud.order.service.TradeOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 交易订单 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，直接注入 Mapper）
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {

    /** 状态流转矩阵：key=当前状态，value=允许流转的目标状态集合 */
    private static final Map<String, Set<String>> TRANSITION_MATRIX = new HashMap<>();

    static {
        TRANSITION_MATRIX.put("PENDING", Set.of("PAID", "CANCELLED"));
        TRANSITION_MATRIX.put("PAID", Set.of("SHIPPED", "REFUNDED"));
        TRANSITION_MATRIX.put("SHIPPED", Set.of("COMPLETED", "REFUNDED"));
        TRANSITION_MATRIX.put("COMPLETED", Set.of("REFUNDED"));
    }

    @Resource
    private TradeOrderMapper tradeOrderMapper;

    @Resource
    private OrderItemMapper orderItemMapper;

    @Resource
    private StockService stockService;

    @Override
    public PageResult<TradeOrderDO> page(PageParam pageParam, String orderNo, Long userId, String status) {
        Page<TradeOrderDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.like(TradeOrderDO::getOrderNo, orderNo);
        }
        if (userId != null) {
            wrapper.eq(TradeOrderDO::getUserId, userId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(TradeOrderDO::getStatus, status);
        }
        wrapper.orderByDesc(TradeOrderDO::getCreateTime);
        Page<TradeOrderDO> result = tradeOrderMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public TradeOrderDO getDetail(Long id) {
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TradeOrderDO order) {
        Long userId = UserContextHolder.getUserId();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
        }
        if (order.getPayAmount() == null) {
            order.setPayAmount(order.getTotalAmount());
        }
        order.setStatus("PENDING");
        tradeOrderMapper.insert(order);
        // 保存订单明细并扣减库存
        List<OrderItemDO> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (OrderItemDO item : items) {
                item.setOrderId(order.getId());
                if (item.getSkuName() == null) {
                    item.setSkuName(item.getSkuCode());
                }
                orderItemMapper.insert(item);
                // 如果订单携带商品信息，扣减对应库存
                if (item.getSkuCode() != null && item.getQuantity() != null) {
                    stockService.deduct(item.getSkuCode(), item.getQuantity());
                }
            }
        }
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TradeOrderDO order) {
        TradeOrderDO exist = tradeOrderMapper.selectById(order.getId());
        if (exist == null || !"PENDING".equals(exist.getStatus())) {
            throw new BusinessException("仅待支付订单可编辑");
        }
        exist.setSubject(order.getSubject());
        exist.setTotalAmount(order.getTotalAmount());
        exist.setPayAmount(order.getPayAmount());
        tradeOrderMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, String payType) {
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        transition(id, "PAID");
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaidByOrderNo(String orderNo, String payType) {
        if (orderNo == null || orderNo.isBlank()) {
            return false;
        }
        TradeOrderDO order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>().eq(TradeOrderDO::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            // 仅支付、无对应业务订单的场景，静默返回
            return false;
        }
        if ("PAID".equals(order.getStatus())) {
            // 幂等：已支付直接返回
            return true;
        }
        if (!"PENDING".equals(order.getStatus())) {
            // 非待支付状态（如已取消），不强制流转
            return false;
        }
        order.setStatus("PAID");
        order.setPayType(payType);
        order.setPayTime(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id) {
        transition(id, "SHIPPED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        transition(id, "COMPLETED");
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        order.setFinishTime(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        transition(id, "CANCELLED");
        // 归还订单关联库存
        returnOrderStock(id);
    }

    @Override
    public void delete(Long id) {
        tradeOrderMapper.deleteById(id);
    }

    @Override
    public List<TradeOrderDO> listByUser(Long userId) {
        return tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .orderByDesc(TradeOrderDO::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        // 仅 PAID/SHIPPED/COMPLETED 可退款，由状态流转矩阵校验
        transition(id, "REFUNDED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long id, String targetStatus) {
        TradeOrderDO order = tradeOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        String currentStatus = order.getStatus();
        Set<String> allowed = TRANSITION_MATRIX.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new BusinessException("订单状态不允许从[" + currentStatus + "]流转到[" + targetStatus + "]");
        }
        order.setStatus(targetStatus);
        tradeOrderMapper.updateById(order);
    }

    @Override
    public Map<String, Object> stats(String startDate, String endDate) {
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(TradeOrderDO::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(TradeOrderDO::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(wrapper);

        long totalOrders = orders.size();
        BigDecimal totalAmount = orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按状态分组
        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (TradeOrderDO o : orders) {
            statusDistribution.merge(o.getStatus(), 1L, Long::sum);
        }

        // 按天分组趋势
        Map<String, Map<String, Object>> dailyTrend = new LinkedHashMap<>();
        for (TradeOrderDO o : orders) {
            if (o.getCreateTime() == null) {
                continue;
            }
            String day = o.getCreateTime().format(dayFmt);
            Map<String, Object> dayStat = dailyTrend.computeIfAbsent(day, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", k);
                m.put("count", 0L);
                m.put("amount", BigDecimal.ZERO);
                return m;
            });
            dayStat.put("count", (Long) dayStat.get("count") + 1);
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            dayStat.put("amount", ((BigDecimal) dayStat.get("amount")).add(amt));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("statusDistribution", statusDistribution);
        result.put("dailyTrend", new ArrayList<>(dailyTrend.values()));
        return result;
    }

    /** 归还订单关联的库存 */
    private void returnOrderStock(Long orderId) {
        List<OrderItemDO> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, orderId));
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItemDO item : items) {
            if (item.getSkuCode() != null && item.getQuantity() != null) {
                stockService.returnStock(item.getSkuCode(), item.getQuantity());
            }
        }
    }

    private String generateOrderNo() {
        return "T" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (System.nanoTime() % 1000);
    }
}
