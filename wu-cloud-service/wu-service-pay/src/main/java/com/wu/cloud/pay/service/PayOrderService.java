package com.wu.cloud.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.infra.redis.service.RedisService;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.mapper.PayOrderMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付订单服务：下单、回调入账、状态同步、对账。
 *
 * <p>资金安全三原则（本类实现要点）：</p>
 * <ol>
 *   <li><b>金额以调用方入参为准</b>：严禁使用测试下单接口，避免真实交易被写成固定小额；</li>
 *   <li><b>入账幂等</b>：分布式锁 + 条件更新（{@code WHERE status='PENDING'}）双保险，
 *       即便回调重复投递或多实例并发也只会入账一次；</li>
 *   <li><b>金额校验</b>：第三方回调/查单返回的金额必须与本地订单一致，防止篡改。</li>
 * </ol>
 */
@Slf4j
@Service
public class PayOrderService {

    /** 待支付 */
    public static final String STATUS_PENDING = "PENDING";
    /** 已支付 */
    public static final String STATUS_PAID = "PAID";
    /** 已关闭 */
    public static final String STATUS_CLOSED = "CLOSED";

    /** 入账分布式锁前缀 */
    private static final String LOCK_PAY_NOTIFY = "pay:notify:";

    @Resource
    private PayOrderMapper payOrderMapper;
    @Resource
    private PayServiceFactory payServiceFactory;
    @Resource
    private RedisService redisService;
    @Resource
    private PayOrderNotifier payOrderNotifier;
    @Resource
    private PayTxnService payTxnService;

    /**
     * 创建支付订单。
     *
     * <p>修复要点：原实现调用 {@code payService.createTestOrder()}，该方法把金额写死为
     * 0.01 元且自行生成订单号，导致「用户支付金额与业务订单金额不一致」的资损问题。
     * 现改为调用 {@link PayService#createOrder(String, BigDecimal, String)} 传入真实金额。</p>
     *
     * @param payType  支付方式 ALIPAY / WECHAT / BALANCE
     * @param amount   支付金额（元），必须大于 0
     * @param orderNo  业务订单号；为空时自动生成（仅适用于无上游业务单的场景）
     * @param subject  商品标题
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> createOrder(String payType, BigDecimal amount, String orderNo, String subject) {
        if (!StringUtils.hasText(payType)) {
            throw new BusinessException("支付方式不能为空");
        }
        // 金额必须显式传入且为正数，杜绝 0 元/负数下单
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额必须大于 0");
        }
        PayService payService = payServiceFactory.getService(payType);

        String finalOrderNo = StringUtils.hasText(orderNo) ? orderNo : generateOrderNo(payType);
        // 同一业务订单号不允许重复下单（已支付时直接拒绝）
        PayOrderDO exist = getByOrderNo(finalOrderNo);
        if (exist != null && STATUS_PAID.equals(exist.getStatus())) {
            throw new BusinessException("该订单已支付，请勿重复下单");
        }

        String finalSubject = StringUtils.hasText(subject) ? subject : "订单-" + finalOrderNo;
        Map<String, String> result = payService.createOrder(finalOrderNo, amount, finalSubject);

        if (exist == null) {
            PayOrderDO payOrder = new PayOrderDO();
            payOrder.setOrderNo(finalOrderNo);
            payOrder.setPayType(payType);
            payOrder.setAmount(amount);
            payOrder.setStatus(STATUS_PENDING);
            payOrderMapper.insert(payOrder);
        } else {
            // 未支付订单允许改价重下（例如用户改了数量）
            payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                    .eq(PayOrderDO::getOrderNo, finalOrderNo)
                    .eq(PayOrderDO::getStatus, STATUS_PENDING)
                    .set(PayOrderDO::getAmount, amount)
                    .set(PayOrderDO::getPayType, payType));
        }

        result.putIfAbsent("orderNo", finalOrderNo);
        result.put("amount", amount.toPlainString());

        // 余额支付无第三方异步回调，下单即入账
        if ("BALANCE".equals(payType) && "SUCCESS".equals(result.get("tradeState"))) {
            handleNotifyInternal(payType, finalOrderNo, result.getOrDefault("transactionId", "BAL" + finalOrderNo));
        }
        return result;
    }

    /**
     * 支付回调入账（由 {@link PayNotifyService} 验签通过后调用）。
     *
     * <p>幂等实现：</p>
     * <ul>
     *   <li>Redisson 分布式锁按 orderNo 串行化，阻断多实例并发重复入账；</li>
     *   <li>条件更新 {@code WHERE order_no=? AND status='PENDING'}，依据影响行数判断，
     *       即便锁失效（如 Redis 抖动）也不会重复入账。</li>
     * </ul>
     */
    public void handleNotifyInternal(String payType, String orderNo, String transactionId) {
        if (!StringUtils.hasText(orderNo)) {
            log.warn("[Pay] 回调订单号为空，忽略");
            return;
        }
        RLock lock = redisService.getLock(LOCK_PAY_NOTIFY + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[Pay] 获取入账锁失败，orderNo={}", orderNo);
                throw new BusinessException("支付处理中，请稍后重试");
            }
            // 仅在本次真正完成状态流转时才通知订单服务，避免重复回调重复通知
            // （markPaidAndEnqueue 在同一本地事务内完成入账+登记本地消息，经代理调用保证事务生效）
            if (payTxnService.markPaidAndEnqueue(payType, orderNo, transactionId)) {
                payOrderNotifier.notifyOrderPaid(orderNo, payType);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("支付处理被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 条件更新为已支付并登记本地消息的逻辑已下沉至 {@link PayTxnService#markPaidAndEnqueue}，
     * 独立 Bean 保证事务注解经代理生效（避免同类自调用导致事务失效）。
     */

    /**
     * 主动查单并同步本地状态（兜底：回调丢失时由前端轮询/定时任务触发）。
     *
     * <p>同时校验第三方返回金额与本地订单金额是否一致，不一致则拒绝入账并告警，
     * 防止「支付 1 元、订单 100 元」类篡改。</p>
     */
    public PayOrderDO syncOrderStatus(String orderNo) {
        PayOrderDO order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("支付订单不存在: " + orderNo);
        }
        if (STATUS_PAID.equals(order.getStatus())) {
            return order;
        }
        try {
            PayService payService = payServiceFactory.getService(order.getPayType());
            Map<String, String> query = payService.queryOrder(orderNo);
            if (query != null && "SUCCESS".equals(query.get("tradeState"))) {
                if (!amountMatched(order, query.get("amount"))) {
                    log.error("[Pay] 查单金额与本地订单不一致，拒绝入账: orderNo={}, local={}, remote={}",
                            orderNo, order.getAmount(), query.get("amount"));
                    throw new BusinessException("支付金额校验失败，请联系客服");
                }
                handleNotifyInternal(order.getPayType(), orderNo, query.get("transactionId"));
                return getByOrderNo(orderNo);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 查单失败不应影响状态查询接口本身，返回本地状态即可
            log.warn("[Pay] 主动查单失败: orderNo={}, err={}", orderNo, e.getMessage());
        }
        return order;
    }

    /**
     * 关闭超时未支付订单。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(String orderNo) {
        PayOrderDO order = getByOrderNo(orderNo);
        if (order == null || !STATUS_PENDING.equals(order.getStatus())) {
            return false;
        }
        try {
            payServiceFactory.getService(order.getPayType()).closeOrder(orderNo);
        } catch (Exception e) {
            log.warn("[Pay] 第三方关单失败（继续关闭本地订单）: orderNo={}, err={}", orderNo, e.getMessage());
        }
        return payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderNo, orderNo)
                .eq(PayOrderDO::getStatus, STATUS_PENDING)
                .set(PayOrderDO::getStatus, STATUS_CLOSED)) > 0;
    }

    /**
     * 仅供本地联调：直接把订单标记为已支付。
     *
     * <p><b>安全说明</b>：该方法是绕过验签的资金入口，其对外端点
     * {@code /api/pay/notify/simulate} 已由网关 {@code PayApiBlockFilter} 屏蔽，
     * 且 Controller 侧以 {@code @Profile("!prod")} 限定，生产环境不会注册。</p>
     */
    public void simulatePaid(String orderNo, String transactionId) {
        log.warn("[Pay] 使用模拟支付入账（仅限非生产环境）: orderNo={}", orderNo);
        PayOrderDO order = getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("支付订单不存在: " + orderNo);
        }
        handleNotifyInternal(order.getPayType(), orderNo, transactionId);
    }

    public PayOrderDO getByOrderNo(String orderNo) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderNo, orderNo)
                .orderByDesc(PayOrderDO::getId)
                .last("LIMIT 1"));
    }

    public List<PayOrderDO> listByOrderNo(String orderNo) {
        return payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>()
                .eq(StringUtils.hasText(orderNo), PayOrderDO::getOrderNo, orderNo)
                .orderByDesc(PayOrderDO::getId));
    }

    /**
     * 对账：统计区间内订单笔数与金额差额。
     */
    public Map<String, Object> reconcile(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
        List<PayOrderDO> orders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>()
                .ge(PayOrderDO::getCreateTime, start)
                .lt(PayOrderDO::getCreateTime, end));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        int paidCount = 0;
        for (PayOrderDO order : orders) {
            BigDecimal amt = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
            totalAmount = totalAmount.add(amt);
            if (STATUS_PAID.equals(order.getStatus())) {
                paidAmount = paidAmount.add(amt);
                paidCount++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("totalCount", orders.size());
        result.put("paidCount", paidCount);
        result.put("unpaidCount", orders.size() - paidCount);
        result.put("totalAmount", totalAmount);
        result.put("paidAmount", paidAmount);
        result.put("diffAmount", totalAmount.subtract(paidAmount));
        return result;
    }

    /**
     * 校验第三方返回金额与本地订单金额是否一致（remote 为空时视为不校验）。
     */
    private boolean amountMatched(PayOrderDO order, String remoteAmount) {
        if (!StringUtils.hasText(remoteAmount) || order.getAmount() == null) {
            return true;
        }
        try {
            return order.getAmount().compareTo(new BigDecimal(remoteAmount)) == 0;
        } catch (NumberFormatException e) {
            log.warn("[Pay] 第三方返回金额无法解析: {}", remoteAmount);
            return true;
        }
    }

    private String generateOrderNo(String payType) {
        String prefix = switch (payType) {
            case "WECHAT" -> "WX";
            case "ALIPAY" -> "ALI";
            case "BALANCE" -> "BAL";
            default -> "PAY";
        };
        return prefix + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }
}
