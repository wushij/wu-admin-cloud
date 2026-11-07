package com.wu.cloud.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 安全审计与反爬拦截事件实体。
 *
 * <p>对应表 {@code sys_security_event}。该表<b>没有</b> {@code deleted} 逻辑删除列，
 * 因此本类不继承 {@code BaseEntity}（继承后 MyBatis-Plus 会自动追加
 * {@code WHERE deleted = 0} 导致 Unknown column 报错），与本服务的
 * {@link SysLoginLogDO} 保持同一范式。</p>
 */
@Data
@TableName("sys_security_event")
public class SecurityEventDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型: UA_BLOCK/ANTI_REPLAY/SIGN_FAIL/TIMESTAMP_INVALID/RATE_LIMIT/RISK_BLOCK/HONEYPOT/PERMISSION_DENIED
     */
    private String eventType;

    /**
     * 主体标识(IP或账号)
     */
    private String principal;

    /**
     * 客户端真实IP
     */
    private String ipaddr;

    /**
     * 请求路径
     */
    private String requestUri;

    /**
     * 请求方法(GET/POST/PUT/DELETE)
     */
    private String requestMethod;

    /**
     * 请求User-Agent头
     */
    private String userAgent;

    /**
     * 本次触发风险分增量
     */
    private Integer riskDelta;

    /**
     * 触发后累计风险分
     */
    private Integer totalRiskScore;

    /**
     * 处置动作: BLOCKED/FORBIDDEN/WARNING/CHALLENGE
     */
    private String action;

    /**
     * 触发原因详细说明
     */
    private String reason;

    /**
     * 触发时间
     */
    private LocalDateTime createTime;
}
