package com.wu.cloud.ticket.testsupport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import static org.mockito.ArgumentMatchers.any;

/**
 * MyBatis-Plus Wrapper 的 Mockito 泛型匹配。
 */
public final class MybatisMockMatchers {

    private MybatisMockMatchers() {
    }

    @SuppressWarnings("unchecked")
    public static <T> LambdaQueryWrapper<T> anyLambdaQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> LambdaUpdateWrapper<T> anyLambdaUpdateWrapper() {
        return any(LambdaUpdateWrapper.class);
    }
}
