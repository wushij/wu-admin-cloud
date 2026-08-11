package com.wu.cloud.api.infra.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 本地消息表 DTO（P1-4 修复）
 * <p>
 * 业务服务完成本地事务后，构造此 DTO 调用 {@code RemoteOutboxService.save()} 写入补偿消息。
 * infra 的 OutboxCompensateJob 会轮询并调用 {@code targetService + targetApi}。
 * </p>
 */
@Data
public class OutboxDTO implements Serializable {

    /** 业务唯一键（幂等，如 pay:order:123456） */
    private String bizKey;

    /** 目标服务名（Nacos 注册名，如 wu-service-order） */
    private String targetService;

    /** 目标 API 路径（如 /inner/order/notify-paid） */
    private String targetApi;

    /** 消息体 JSON */
    private String payload;
}
