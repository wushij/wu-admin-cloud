package com.wu.cloud.common.utils.bean;

import cn.hutool.core.bean.BeanUtil;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对象 Bean 拷贝工具类
 */
public class BeanMappingUtils {

    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetClass);
    }

    public static <S, T> List<T> copyListProperties(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> copyProperties(source, targetClass))
                .collect(Collectors.toList());
    }
}
