package com.wu.cloud.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表 DO（P1-4 修复）
 * <p>
 * 业务服务完成本地事务后，通过 Feign 调用 infra 写入一条 outbox 记录。
 * infra 的 {@link com.wu.cloud.infra.job.OutboxCompensateJob} 定时轮询 PENDING 记录，
 * 通过 @LoadBalanced RestTemplate 调用目标服务的 API。
 * 通过 biz_key 实现幂等，retry_count + 指数退避实现重试，MAX_RETRY 后标记 DEAD。
 * </p>
 */
@Data
@TableName("infra_outbox")
public class OutboxDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一键（幂等，如 pay:order:123456） */
    private String bizKey;

    /** 目标服务名（Nacos 注册名，如 wu-service-order） */
    private String targetService;

    /** 目标 API 路径（如 /inner/order/notify-paid） */
    private String targetApi;

    /** 消息体 JSON */
    private String payload;

    /** 状态：PENDING / SUCCESS / DEAD */
    private String status;

    /** 重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
