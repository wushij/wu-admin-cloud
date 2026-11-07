package com.wu.cloud.auth.support;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.common.utils.crypto.Sm2Util;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 登录/注册口令传输解密服务（与单体新版对齐）。
 *
 * <p>前端在接口安全开启时使用 SM2 公钥（C1C3C2）加密密码后再提交，服务端必须在
 * BCrypt 校验前还原明文。私钥来源优先级：
 * <ol>
 *   <li>系统配置 security 分组的 {@code sm2PrivateKey}（运维显式托管）；</li>
 *   <li>{@link Sm2KeyManager} 托管的运行时共享密钥对（Redis 多实例一致）。</li>
 * </ol>
 * 未加密或非 SM2 密文的输入会原样返回，保证向后兼容。</p>
 */
@Component
public class PasswordCipherService {

    @Resource
    private AuthConfigLoader authConfigLoader;

    @Resource
    private Sm2KeyManager sm2KeyManager;

    /** 对外下发的 SM2 公钥：显式配置优先，否则使用运行时密钥对公钥 */
    public String getPublicKey() {
        String configured = authConfigLoader.getSm2PublicKey();
        return StrUtil.isNotBlank(configured) ? configured.trim() : sm2KeyManager.getPublicKey();
    }

    /** 服务端解密私钥：显式配置优先，否则使用运行时密钥对私钥（绝不外发） */
    public String getPrivateKey() {
        String configured = authConfigLoader.getSm2PrivateKey();
        return StrUtil.isNotBlank(configured) ? configured.trim() : sm2KeyManager.getPrivateKey();
    }

    /** 解密客户端提交的口令；未加密时原样返回 */
    public String resolve(String rawPassword) {
        if (StrUtil.isBlank(rawPassword)) {
            return rawPassword;
        }
        return Sm2Util.decryptPasswordIfEncrypted(getPrivateKey(), rawPassword);
    }
}
