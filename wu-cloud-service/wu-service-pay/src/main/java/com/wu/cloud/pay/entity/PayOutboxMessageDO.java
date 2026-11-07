package com.wu.cloud.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付结果通知本地消息表（Transactional Outbox）。
 * <p>
 * 与 pay_order 同库同事务写入：支付单状态流转为 PAID 的同一个本地事务内
 * 插入 PENDING 记录，事务提交后异步投递到订单服务，失败由补偿任务重试，
 * 保证「已入账必通知」的最终一致性。
 * </p>
 */
@Data
@TableName("pay_outbox_message")
public class PayOutboxMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_DEAD = "DEAD";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一键：pay:order:paid:{orderNo}，用于投递去重与排查 */
    private String bizKey;

    /** 业务订单号 */
    private String orderNo;

    /** 支付方式 */
    private String payType;

    /** 投递状态：PENDING / SUCCESS / DEAD */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间（指数退避） */
    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
