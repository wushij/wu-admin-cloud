package com.wu.cloud.infra.service;

import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 通过 Feign 解析用户名称（微服务无本地 UserMapper）
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
                if (u.getUsername() != null && !u.getUsername().isBlank()) {
                    return u.getUsername();
                }
                return u.getRealName();
            }
        } catch (Exception ignored) {
            // Feign 失败降级，不阻断主流程
        }
        return "用户" + userId;
    }

    public Map<Long, String> batchResolve(Collection<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (Long id : ids) {
            if (id != null) {
                map.put(id, resolve(id));
            }
        }
        return map;
    }

    public void fillApiAccessUsernames(Iterable<com.wu.cloud.infra.entity.ApiAccessLogDO> records) {
        if (records == null) {
            return;
        }
        Map<Long, String> nameCache = new HashMap<>();
        for (com.wu.cloud.infra.entity.ApiAccessLogDO log : records) {
            if (log == null) {
                continue;
            }
            if (log.getUserId() == null) {
                log.setUsername("-");
                continue;
            }
            String name = nameCache.computeIfAbsent(log.getUserId(), this::resolve);
            log.setUsername(Objects.requireNonNullElse(name, "用户" + log.getUserId()));
        }
    }
}
