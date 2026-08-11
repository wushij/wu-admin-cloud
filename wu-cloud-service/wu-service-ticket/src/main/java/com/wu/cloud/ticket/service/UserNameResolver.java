package com.wu.cloud.ticket.service;

import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通过 Feign 解析用户名称（替代单体本地 UserMapper）
 */
@Component
public class UserNameResolver {

    @Resource
    private RemoteUserService remoteUserService;

    public String resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            R<SysUserDTO> r = remoteUserService.getUserById(userId);
            if (r != null && r.isSuccess() && r.getData() != null) {
                SysUserDTO u = r.getData();
                return u.getRealName() != null ? u.getRealName() : u.getUsername();
            }
        } catch (Exception ignored) {
            // Feign 调用失败降级为空，避免阻断主流程
        }
        return null;
    }

    public <T> void fillNames(Iterable<T> list, Function<T, Long> idGetter, java.util.function.BiConsumer<T, String> nameSetter) {
        for (T item : list) {
            nameSetter.accept(item, resolve(idGetter.apply(item)));
        }
    }

    public Map<Long, String> batchResolve(java.util.Collection<Long> ids) {
        return ids.stream().collect(Collectors.toMap(id -> id, this::resolve, (a, b) -> b));
    }
}
