package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthConfigLoader;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.mail.MailSender;
import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 验证码服务实现：负责生成、限频、下发与校验
 */
@Slf4j
@Service
public class VerifyCodeServiceImpl implements VerifyCodeService {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private AuthProperties authProperties;

    @Resource
    private AuthConfigLoader authConfigLoader;

    @Resource
    private SmsSender smsSender;

    @Resource
    private MailSender mailSender;

    @Override
    public void sendSmsCode(String scene, String mobile) {
        validateMobile(mobile);
        checkSendInterval(scene, mobile);

        String code = randomCode();
        redisTemplate.opsForValue().set(
                AuthRedisKeys.smsCode(scene, mobile),
                code,
                Duration.ofSeconds(authProperties.getCodeExpireSeconds()));

        long expireMinutes = Math.max(1, authProperties.getCodeExpireSeconds() / 60);
        SmsResult result = smsSender.send(mobile, scene,
                Map.of("code", code, "minute", String.valueOf(expireMinutes)));
        if (!result.isSuccess()) {
            // 发送失败时清理验证码与间隔标记，允许用户立即重试
            redisTemplate.delete(AuthRedisKeys.smsCode(scene, mobile));
            redisTemplate.delete(AuthRedisKeys.codeInterval(scene, mobile));
            throw new BusinessException("短信发送失败：" + result.getMessage());
        }
        markSendInterval(scene, mobile);
    }

    @Override
    public void sendEmailCode(String scene, String email) {
        validateEmail(email);
        checkSendInterval(scene, email);

        String code = randomCode();
        redisTemplate.opsForValue().set(
                AuthRedisKeys.emailCode(scene, email),
                code,
                Duration.ofSeconds(authProperties.getCodeExpireSeconds()));

        long expireMinutes = Math.max(1, authProperties.getCodeExpireSeconds() / 60);
        String subject = "【系统通知】验证码";
        String content = String.format(
                "您的验证码是 %s，%d 分钟内有效，请勿泄露给他人。", code, expireMinutes);
        var result = mailSender.send(email, subject, content);
        if (!result.isSuccess()) {
            redisTemplate.delete(AuthRedisKeys.emailCode(scene, email));
            redisTemplate.delete(AuthRedisKeys.codeInterval(scene, email));
            throw new BusinessException("邮件发送失败：" + result.getMessage());
        }
        markSendInterval(scene, email);
    }

    @Override
    public void verifySmsCode(String scene, String mobile, String code) {
        doVerify(AuthRedisKeys.smsCode(scene, mobile), code);
    }

    @Override
    public void verifyEmailCode(String scene, String email, String code) {
        doVerify(AuthRedisKeys.emailCode(scene, email), code);
    }

    @Override
    public String verifyAndIssueTicket(String scene, String target, String code, boolean isSms) {
        if (isSms) {
            verifySmsCode(scene, target, code);
        } else {
            verifyEmailCode(scene, target, code);
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                AuthRedisKeys.codeTicket(scene, ticket),
                target,
                Duration.ofSeconds(authProperties.getCodeExpireSeconds()));
        return ticket;
    }

    @Override
    public String consumeTicket(String scene, String ticket) {
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException("校验凭证不能为空");
        }
        String key = AuthRedisKeys.codeTicket(scene, ticket);
        String target = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (target == null) {
            throw new BusinessException("校验凭证已失效，请重新验证");
        }
        return target;
    }

    private void doVerify(String key, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("请输入验证码");
        }
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code.trim())) {
            throw new BusinessException("验证码错误");
        }
        redisTemplate.delete(key);
    }

    /**
     * 限制同一目标的发送频率
     */
    private void checkSendInterval(String scene, String target) {
        Long ttl = redisTemplate.getExpire(AuthRedisKeys.codeInterval(scene, target));
        if (ttl != null && ttl > 0) {
            throw new BusinessException("发送过于频繁，请 " + ttl + " 秒后重试");
        }
    }

    private void markSendInterval(String scene, String target) {
        redisTemplate.opsForValue().set(
                AuthRedisKeys.codeInterval(scene, target),
                "1",
                Duration.ofSeconds(authConfigLoader.getSmsSendIntervalSeconds()));
    }

    private void validateMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || !MOBILE_PATTERN.matcher(mobile).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
    }

    private String randomCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
