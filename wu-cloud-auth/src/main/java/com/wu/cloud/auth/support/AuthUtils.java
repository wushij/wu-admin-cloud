package com.wu.cloud.auth.support;

import com.wu.cloud.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 认证相关公共工具：脱敏、密码校验与加密
 */
public final class AuthUtils {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");

    private AuthUtils() {
    }

    /**
     * 手机号脱敏：138****8000
     */
    public static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 邮箱脱敏：ab***@example.com
     */
    public static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 2) + "***" + domain;
    }

    public static boolean isMobile(String mobile) {
        return StringUtils.hasText(mobile) && MOBILE_PATTERN.matcher(mobile).matches();
    }

    public static boolean isEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 校验账号格式
     */
    public static void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("账号不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("账号只能包含字母、数字、下划线和短横线，长度 3-30 位");
        }
    }

    /**
     * 校验新密码与确认密码
     */
    public static void validatePassword(String password, String confirmPassword, int minLength) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < minLength) {
            throw new BusinessException("密码长度不能少于 " + minLength + " 位");
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            throw new BusinessException("两次输入的密码不一致");
        }
    }

    private static final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder BCRYPT_ENCODER =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    /**
     * 生成 BCrypt 密文
     */
    public static String encryptPassword(String rawPassword) {
        return BCRYPT_ENCODER.encode(rawPassword);
    }

    /**
     * 校验密码，兼容历史明文数据
     */
    public static boolean matchPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            try {
                return BCRYPT_ENCODER.matches(rawPassword, storedPassword);
            } catch (Exception ignored) {
                return false;
            }
        }
        // 历史明文数据兼容
        return rawPassword.equals(storedPassword);
    }
}
