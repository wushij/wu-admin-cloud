package com.wu.cloud.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserRegisterDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.dto.ForgotPasswordCheckDTO;
import com.wu.cloud.auth.dto.ForgotPasswordResetDTO;
import com.wu.cloud.auth.dto.ForgotPasswordSmsCodeDTO;
import com.wu.cloud.auth.dto.LoginQueryDTO;
import com.wu.cloud.auth.dto.RegisterDTO;
import com.wu.cloud.auth.service.AuthService;
import com.wu.cloud.auth.service.CaptchaService;
import com.wu.cloud.auth.service.LoginLockService;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.auth.support.AuthUtils;
import com.wu.cloud.auth.support.UserAgentUtils;
import com.wu.cloud.auth.vo.ForgotPasswordCheckVO;
import com.wu.cloud.auth.vo.LoginConfigVO;
import com.wu.cloud.auth.vo.LoginVO;
import com.wu.cloud.auth.vo.SessionSignInitVO;
import com.wu.cloud.auth.vo.UserInfoVO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.utils.web.ClientIpUtils;
import com.wu.cloud.common.utils.web.IpLocationUtils;
import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 认证中心服务实现：登录、注册、忘记密码与登录配置
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** 短信登录方式标识 */
    private static final String LOGIN_TYPE_SMS = "sms";

    /** 账号禁用状态值，与 sys_user.status 定义一致（0 禁用 1 启用 2 待审核 3 审核未通过） */
    private static final int USER_STATUS_DISABLED = 0;
    /** 待审核状态：开启注册审核后，新注册用户须管理员审核通过后方可登录 */
    private static final int USER_STATUS_PENDING_AUDIT = 2;
    /** 审核未通过状态 */
    private static final int USER_STATUS_AUDIT_REJECTED = 3;

    /** 登录日志状态：成功（与 sys_login_log.status 定义一致） */
    private static final int LOG_SUCCESS = 0;

    /** 登录日志状态：失败 */
    private static final int LOG_FAIL = 1;

    @Resource
    private RemoteUserService remoteUserService;

    @Resource
    private RemoteLoginLogService remoteLoginLogService;

    @Resource
    private RemoteOnlineUserService remoteOnlineUserService;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private VerifyCodeService verifyCodeService;

    @Resource
    private LoginLockService loginLockService;

    @Resource
    private AuthProperties authProperties;

    @Resource
    private com.wu.cloud.auth.config.AuthConfigLoader authConfigLoader;

    @Resource
    private com.wu.cloud.auth.support.PasswordCipherService passwordCipherService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private HttpServletRequest request;

    @Override
    public LoginVO login(LoginQueryDTO query) {
        if (query == null) {
            throw new BusinessException("登录参数不能为空");
        }
        return LOGIN_TYPE_SMS.equalsIgnoreCase(query.getLoginType())
                ? loginBySms(query)
                : loginByPassword(query);
    }

    /**
     * 账号密码登录
     */
    private LoginVO loginByPassword(LoginQueryDTO query) {
        String username = query.getUsername();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(query.getPassword())) {
            throw new BusinessException("用户名或密码不能为空");
        }

        // 先做人机校验，避免验证码被绕过
        verifyHumanChallenge(query);

        loginLockService.assertNotLocked(username);

        LoginUserDTO user = findUserByUsername(username);
        if (user == null) {
            int remain = loginLockService.recordFailure(username);
            recordLoginLog(null, username, LOG_FAIL, "账号不存在");
            throw new BusinessException(buildFailMessage("账号或密码错误", remain));
        }

        if (!AuthUtils.matchPassword(passwordCipherService.resolve(query.getPassword()), user.getPassword())) {
            int remain = loginLockService.recordFailure(username);
            recordLoginLog(user.getUserId(), username, LOG_FAIL, "密码错误");
            throw new BusinessException(buildFailMessage("账号或密码错误", remain));
        }

        assertUserEnabled(user);

        loginLockService.clearFailure(username);
        return doLogin(user);
    }

    /**
     * 短信验证码登录
     */
    private LoginVO loginBySms(LoginQueryDTO query) {
        if (!authConfigLoader.isSmsLoginEnabled() || !authConfigLoader.isSmsEnabled()) {
            throw new BusinessException("短信登录未开启");
        }
        String mobile = query.getMobile();
        if (!AuthUtils.isMobile(mobile)) {
            throw new BusinessException("手机号格式不正确");
        }

        verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_SMS_LOGIN, mobile, query.getSmsCode());

        R<LoginUserDTO> result = remoteUserService.getUserByMobile(mobile);
        LoginUserDTO user = result == null ? null : result.getData();
        if (user == null) {
            recordLoginLog(null, mobile, LOG_FAIL, "手机号未绑定账号");
            throw new BusinessException("该手机号未绑定账号");
        }

        assertUserEnabled(user);

        loginLockService.clearFailure(user.getUsername());
        return doLogin(user);
    }

    /**
     * 执行登录并组装返回结果（与旧版 completeLogin 平铺格式对齐）
     */
    private LoginVO doLogin(LoginUserDTO user) {
        boolean concurrent = authConfigLoader.isConcurrentLogin();
        if (!concurrent) {
            StpUtil.logout(user.getUserId());
        }
        StpUtil.login(user.getUserId());
        // 将角色与权限写入 Session，供网关与业务服务下发上下文
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("nickname", user.getNickname());
        StpUtil.getSession().set("roles", user.getRoles());
        StpUtil.getSession().set("permissions", user.getPermissions());

        recordLoginLog(user.getUserId(), user.getUsername(), LOG_SUCCESS, "登录成功");
        recordOnlineSession(user);

        // 平铺字段，与旧版 completeLogin 返回结构一致（前端读 res.data.userId / res.data.token）
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setTokenName(StpUtil.getTokenName());
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(user.getRoles());
        vo.setPermissions(user.getPermissions());
        return vo;
    }

    @Override
    public UserInfoVO getUserInfo() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("未登录或登录已过期");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String username = (String) StpUtil.getSession().get("username");

        // username 缺失时先补齐，再与菜单并行 Feign
        if (!StringUtils.hasText(username) && userId != null) {
            try {
                R<SysUserDTO> basic = remoteUserService.getUserById(userId);
                if (basic != null && basic.getData() != null && StringUtils.hasText(basic.getData().getUsername())) {
                    username = basic.getData().getUsername();
                }
            } catch (Exception e) {
                log.error("[认证] getUserInfo 按 userId 补用户名失败 userId={}", userId, e);
                throw new BusinessException("用户服务暂不可用，请稍后重试");
            }
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户信息不存在");
        }

        final String resolvedUsername = username;
        CompletableFuture<LoginUserDTO> userFuture = CompletableFuture.supplyAsync(() -> {
            try {
                R<LoginUserDTO> result = remoteUserService.getUserInfoByUsername(resolvedUsername);
                return result == null ? null : result.getData();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture<List<SysMenuDTO>> menusFuture = CompletableFuture.supplyAsync(() -> {
            try {
                R<List<SysMenuDTO>> menuResult = remoteUserService.getMenusByUserId(userId);
                return (menuResult != null && menuResult.getData() != null)
                        ? menuResult.getData() : Collections.emptyList();
            } catch (Exception e) {
                log.warn("[认证] getUserInfo 获取菜单失败 userId={}, 返回空菜单", userId);
                return Collections.emptyList();
            }
        });

        LoginUserDTO user;
        try {
            user = userFuture.join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("[认证] getUserInfo 查询用户失败 userId={}", userId, cause);
            throw new BusinessException("用户服务暂不可用，请稍后重试");
        }
        if (user == null) {
            throw new BusinessException("用户信息不存在");
        }

        List<SysMenuDTO> menus = menusFuture.join();

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(user.getRoles());
        vo.setPermissions(user.getPermissions());
        vo.setMenus(menus);
        return vo;
    }

    /**
     * 人机校验：滑块票据优先，其次图形验证码
     */
    private void verifyHumanChallenge(LoginQueryDTO query) {
        // 登录写路径强一致：先刷 login 组，再读开关
        authConfigLoader.refreshLoginGroup();
        if (!authConfigLoader.isCaptchaEnabled()) {
            return;
        }
        if (StringUtils.hasText(query.getSliderTicket())) {
            captchaService.verifySliderTicket(query.getSliderTicket());
            return;
        }
        captchaService.verifyImageCaptcha(query.getUuid(), query.getCaptcha());
    }

    private void assertUserEnabled(LoginUserDTO user) {
        if (user.getStatus() == null) {
            return;
        }
        if (user.getStatus() == USER_STATUS_DISABLED) {
            recordLoginLog(user.getUserId(), user.getUsername(), LOG_FAIL, "账号已停用");
            throw new BusinessException("账号已停用，请联系管理员");
        }
        if (user.getStatus() == USER_STATUS_PENDING_AUDIT) {
            recordLoginLog(user.getUserId(), user.getUsername(), LOG_FAIL, "账号待审核");
            throw new BusinessException("账号正在等待管理员审核，审核通过后方可登录");
        }
        if (user.getStatus() == USER_STATUS_AUDIT_REJECTED) {
            recordLoginLog(user.getUserId(), user.getUsername(), LOG_FAIL, "注册审核未通过");
            throw new BusinessException("注册审核未通过，请联系管理员");
        }
    }

    private String buildFailMessage(String base, int remain) {
        if (remain <= 0) {
            long minutes = Math.max(1, authConfigLoader.getLockTimeMinutes());
            return base + "，账号已被锁定 " + minutes + " 分钟";
        }
        return base + "，还可尝试 " + remain + " 次";
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public LoginConfigVO getLoginConfig() {
        // 缓存齐全且未过 TTL 时跳过全量 Feign；否则并行刷新（可配置回退）
        authConfigLoader.ensurePublicGroupsForLoginConfig();

        LoginConfigVO vo = new LoginConfigVO();

        LoginConfigVO.SiteSection site = new LoginConfigVO.SiteSection();
        site.setPlatformName(authConfigLoader.getPlatformName());
        site.setPlatformSubtitle(authConfigLoader.getPlatformSubtitle());
        site.setLoginWelcome(authConfigLoader.getLoginWelcome());
        site.setRegisterTitle(authConfigLoader.getRegisterTitle());
        site.setCopyright(authConfigLoader.getCopyright());
        vo.setSite(site);

        LoginConfigVO.LoginSection login = new LoginConfigVO.LoginSection();
        login.setCaptchaEnabled(authConfigLoader.isCaptchaEnabled());
        login.setCaptchaType(authConfigLoader.getCaptchaType());
        login.setSmsLoginEnabled(authConfigLoader.isSmsLoginEnabled());
        login.setSmsLoginSliderCaptchaEnabled(authConfigLoader.isSmsLoginSliderCaptchaEnabled());
        login.setEmailLoginEnabled(authConfigLoader.isEmailLoginEnabled());
        login.setEmailLoginSliderCaptchaEnabled(authConfigLoader.isEmailLoginSliderCaptchaEnabled());
        login.setRememberMe(authConfigLoader.isRememberMeEnabled());
        login.setMaxRetryCount(authConfigLoader.getMaxRetryCount());
        login.setMaxRetryCountIp(authConfigLoader.getMaxRetryCountIp());
        login.setLockTime(authConfigLoader.getLockTimeMinutes());
        login.setSmsEnabled(authConfigLoader.isSmsEnabled());
        login.setEmailEnabled(authConfigLoader.isEmailEnabled());
        vo.setLogin(login);

        LoginConfigVO.RegisterSection register = new LoginConfigVO.RegisterSection();
        register.setEnabled(authConfigLoader.isRegisterEnabled());
        register.setCaptchaEnabled(authConfigLoader.isRegisterCaptchaEnabled());
        register.setCaptchaType(authConfigLoader.getRegisterCaptchaType());
        register.setDefaultRoleCode(authConfigLoader.getRegisterDefaultRoleCode());
        register.setNeedAudit(authConfigLoader.isRegisterNeedAudit());
        register.setMinPasswordLength(authConfigLoader.getRegisterMinPasswordLength());
        vo.setRegister(register);

        boolean sm3SignActive = authConfigLoader.isSm2SignEffective();
        boolean sm4Active = authConfigLoader.isSm4EncryptEffective();
        LoginConfigVO.SecuritySection security = new LoginConfigVO.SecuritySection();
        security.setDisableDevtool(authConfigLoader.isDisableDevtool());
        security.setSm3SignEnabled(sm3SignActive);
        security.setSm2SignEnabled(sm3SignActive);
        security.setSm4EncryptEnabled(sm4Active);
        // 口令传输公钥：即使接口安全开关未开启也下发，前端可据此选择加密提交
        security.setSm2PublicKey(passwordCipherService.getPublicKey());
        vo.setSecurity(security);

        LoginConfigVO.AiSection ai = new LoginConfigVO.AiSection();
        ai.setAssistantEnabled(authConfigLoader.isAiAssistantEnabled());
        vo.setAi(ai);

        return vo;
    }

    @Override
    public SessionSignInitVO initSessionSign(String clientId) {
        // 校验 clientId 与旧版保持一致：8-128 位，否则拒绝
        if (!StringUtils.hasText(clientId) || clientId.length() < 8 || clientId.length() > 128) {
            throw new BusinessException("客户端标识不合法");
        }

        boolean signActive = authConfigLoader.isSm2SignEffective();
        boolean sm4Active = authConfigLoader.isSm4EncryptEffective();
        boolean enabled = signActive || sm4Active;
        SessionSignInitVO vo = new SessionSignInitVO();
        vo.setEnabled(enabled);
        if (!enabled) {
            return vo;
        }

        Duration ttl = Duration.ofMinutes(authConfigLoader.getSessionSignTtlMinutes());

        if (signActive) {
            String sm3SignKey = RandomUtil.randomString("0123456789abcdef", 64);
            redisTemplate.opsForValue().set(AuthRedisKeys.securitySessionSign(clientId), sm3SignKey, ttl);
            vo.setSm3SignKey(sm3SignKey);
        }
        if (sm4Active) {
            String sm4Key = RandomUtil.randomString("0123456789abcdef", 32);
            redisTemplate.opsForValue().set(AuthRedisKeys.securitySessionSm4(clientId), sm4Key, ttl);
            vo.setSm4Key(sm4Key);
        }
        return vo;
    }

    @Override
    public Long register(RegisterDTO registerDTO) {
        if (!authConfigLoader.isRegisterEnabled()) {
            throw new BusinessException("系统未开放注册");
        }
        if (registerDTO == null) {
            throw new BusinessException("注册参数不能为空");
        }

        AuthUtils.validateUsername(registerDTO.getUsername());
        // SM2 口令传输：先解密再校验与加密，密文随机性决定的差异不能影响两次输入一致性判断
        String plainPassword = passwordCipherService.resolve(registerDTO.getPassword());
        String plainConfirmPassword = passwordCipherService.resolve(registerDTO.getConfirmPassword());
        AuthUtils.validatePassword(plainPassword,
                plainConfirmPassword, authConfigLoader.getRegisterMinPasswordLength());

        if (authConfigLoader.isRegisterCaptchaEnabled()) {
            captchaService.verifyImageCaptcha(registerDTO.getUuid(), registerDTO.getCaptcha());
        }

        // 手机号选填，一旦填写则必须通过短信验证
        if (StringUtils.hasText(registerDTO.getMobile())) {
            if (!AuthUtils.isMobile(registerDTO.getMobile())) {
                throw new BusinessException("手机号格式不正确");
            }
            verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_REGISTER,
                    registerDTO.getMobile(), registerDTO.getSmsCode());
        }

        if (StringUtils.hasText(registerDTO.getEmail()) && !AuthUtils.isEmail(registerDTO.getEmail())) {
            throw new BusinessException("邮箱格式不正确");
        }

        if (findUserByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException("账号已存在");
        }

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(registerDTO.getUsername());
        dto.setPassword(AuthUtils.encryptPassword(plainPassword));
        dto.setNickname(StringUtils.hasText(registerDTO.getNickname())
                ? registerDTO.getNickname() : registerDTO.getUsername());
        dto.setMobile(registerDTO.getMobile());
        dto.setEmail(registerDTO.getEmail());

        R<Long> result = remoteUserService.registerUser(dto);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(result == null ? "注册失败" : result.getMsg());
        }
        return result.getData();
    }

    @Override
    public ForgotPasswordCheckVO forgotPasswordCheck(ForgotPasswordCheckDTO checkDTO) {
        if (checkDTO == null || !StringUtils.hasText(checkDTO.getUsername())) {
            throw new BusinessException("请输入账号");
        }
        LoginUserDTO user = findUserByUsername(checkDTO.getUsername());

        ForgotPasswordCheckVO vo = new ForgotPasswordCheckVO();
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (!StringUtils.hasText(user.getMobile())) {
            throw new BusinessException("该账号未绑定手机号，请联系管理员重置密码");
        }
        vo.setExists(true);
        vo.setMaskedMobile(AuthUtils.maskMobile(user.getMobile()));
        return vo;
    }

    @Override
    public String forgotPasswordSmsCode(ForgotPasswordSmsCodeDTO smsCodeDTO) {
        if (smsCodeDTO == null || !StringUtils.hasText(smsCodeDTO.getUsername())) {
            throw new BusinessException("请输入账号");
        }
        LoginUserDTO user = findUserByUsername(smsCodeDTO.getUsername());
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (!StringUtils.hasText(user.getMobile())) {
            throw new BusinessException("该账号未绑定手机号，请联系管理员重置密码");
        }
        verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_FORGOT_PASSWORD, user.getMobile());
        return AuthUtils.maskMobile(user.getMobile());
    }

    @Override
    public void forgotPasswordReset(ForgotPasswordResetDTO resetDTO) {
        if (resetDTO == null || !StringUtils.hasText(resetDTO.getUsername())) {
            throw new BusinessException("请输入账号");
        }
        String plainNewPassword = passwordCipherService.resolve(resetDTO.getNewPassword());
        String plainConfirmPassword = passwordCipherService.resolve(resetDTO.getConfirmPassword());
        AuthUtils.validatePassword(plainNewPassword,
                plainConfirmPassword, authConfigLoader.getRegisterMinPasswordLength());

        LoginUserDTO user = findUserByUsername(resetDTO.getUsername());
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (!StringUtils.hasText(user.getMobile())) {
            throw new BusinessException("该账号未绑定手机号");
        }

        verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_FORGOT_PASSWORD,
                user.getMobile(), resetDTO.getCode());

        R<Boolean> result = remoteUserService.resetPassword(user.getUserId(),
                AuthUtils.encryptPassword(plainNewPassword));
        if (result == null || !result.isSuccess()) {
            throw new BusinessException(result == null ? "密码重置失败" : result.getMsg());
        }

        // 重置成功后解除锁定并踢下线
        loginLockService.clearFailure(user.getUsername());
        try {
            StpUtil.logout(user.getUserId());
        } catch (Exception ignored) {
            // 用户未在线时忽略
        }
    }

    /**
     * 按账号查询用户，远程异常时向上抛出友好提示
     */
    private LoginUserDTO findUserByUsername(String username) {
        try {
            R<LoginUserDTO> result = remoteUserService.getUserInfoByUsername(username);
            return result == null ? null : result.getData();
        } catch (Exception e) {
            log.error("[认证] 查询用户失败 username={}", username, e);
            throw new BusinessException("用户服务暂不可用，请稍后重试");
        }
    }

    /**
     * 记录登录日志，失败不阻断主流程
     */
    private void recordLoginLog(Long userId, String username, Integer status, String message) {
        try {
            String userAgent = request.getHeader("User-Agent");

            LoginLogDTO logDTO = new LoginLogDTO();
            logDTO.setUserId(userId);
            logDTO.setUsername(username);
            String clientIp = ClientIpUtils.resolve(request);
            logDTO.setIp(clientIp);
            logDTO.setAddress(IpLocationUtils.resolve(clientIp));
            logDTO.setBrowser(UserAgentUtils.parseBrowser(userAgent));
            logDTO.setOs(UserAgentUtils.parseOs(userAgent));
            logDTO.setStatus(status);
            logDTO.setMessage(message);
            logDTO.setLoginTime(LocalDateTime.now());
            remoteLoginLogService.saveLoginLog(logDTO);
        } catch (Exception e) {
            log.warn("[认证] 登录日志记录失败 username={}", username);
        }
    }

    private void recordOnlineSession(LoginUserDTO user) {
        try {
            RecordLoginSessionDTO dto = new RecordLoginSessionDTO();
            dto.setUserId(user.getUserId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setClientIp(ClientIpUtils.resolve(request));
            dto.setUserAgent(request.getHeader("User-Agent"));
            remoteOnlineUserService.recordLoginSession(dto);
        } catch (Exception e) {
            log.warn("[认证] 在线会话登记失败 userId={}", user.getUserId());
        }
    }
}
