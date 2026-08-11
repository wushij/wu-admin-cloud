package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 会话签名初始化返回对象，与单体前端 {@code security-config.ts} 期望结构保持一致：
 * <ul>
 *     <li>{@code enabled}：是否启用签名/加密（前端 {@code request.ts} 据此决定是否生效）</li>
 *     <li>{@code sm3SignKey}：HMAC-SM3 签名密钥（64 位 Hex）</li>
 *     <li>{@code sm4Key}：SM4 加解密密钥（32 位 Hex）</li>
 * </ul>
 */
@Data
public class SessionSignInitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否启用签名/加密 */
    private Boolean enabled;

    /** HMAC-SM3 签名密钥（64 位 Hex 字符串） */
    private String sm3SignKey;

    /** SM4 加解密密钥（32 位 Hex 字符串） */
    private String sm4Key;
}
