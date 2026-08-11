package com.wu.cloud.auth.service;

/**
 * 登录失败锁定服务：防止账号被暴力破解
 */
public interface LoginLockService {

    /**
     * 校验账号是否处于锁定状态，锁定时抛出业务异常
     */
    void assertNotLocked(String username);

    /**
     * 记录一次登录失败，达到阈值后锁定账号
     *
     * @return 剩余可尝试次数，0 表示已锁定
     */
    int recordFailure(String username);

    /**
     * 登录成功后清除失败计数
     */
    void clearFailure(String username);
}
