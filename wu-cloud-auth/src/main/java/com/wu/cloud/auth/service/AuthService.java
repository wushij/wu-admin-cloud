package com.wu.cloud.auth.service;

import com.wu.cloud.auth.dto.ForgotPasswordCheckDTO;
import com.wu.cloud.auth.dto.ForgotPasswordResetDTO;
import com.wu.cloud.auth.dto.ForgotPasswordSmsCodeDTO;
import com.wu.cloud.auth.dto.LoginQueryDTO;
import com.wu.cloud.auth.dto.RegisterDTO;
import com.wu.cloud.auth.vo.ForgotPasswordCheckVO;
import com.wu.cloud.auth.vo.LoginConfigVO;
import com.wu.cloud.auth.vo.LoginVO;
import com.wu.cloud.auth.vo.SessionSignInitVO;
import com.wu.cloud.auth.vo.UserInfoVO;

/**
 * 认证中心服务接口
 */
public interface AuthService {

    /**
     * 用户登录（支持账号密码与短信验证码）
     */
    LoginVO login(LoginQueryDTO query);

    /**
     * 获取当前登录用户信息（含角色 / 权限 / 菜单）
     */
    UserInfoVO getUserInfo();

    /**
     * 退出登录
     */
    void logout();

    /**
     * 获取登录页配置
     */
    LoginConfigVO getLoginConfig();

    /**
     * 初始化会话签名，返回会话级动态密钥供前端签名/加密。
     * <p>与旧版前端 {@code security-config.ts} 期望结构一致（enabled / sm3SignKey / sm4Key）。
     *
     * @param clientId 客户端唯一标识（前端生成并随请求头 X-Client-Id 传递）
     */
    SessionSignInitVO initSessionSign(String clientId);

    /**
     * 用户注册
     *
     * @return 新用户主键
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 忘记密码-校验账号并返回脱敏手机号
     */
    ForgotPasswordCheckVO forgotPasswordCheck(ForgotPasswordCheckDTO checkDTO);

    /**
     * 忘记密码-向账号绑定手机号发送验证码
     *
     * @return 脱敏手机号
     */
    String forgotPasswordSmsCode(ForgotPasswordSmsCodeDTO smsCodeDTO);

    /**
     * 忘记密码-校验验证码并重置密码
     */
    void forgotPasswordReset(ForgotPasswordResetDTO resetDTO);
}
