package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteLoginLogServiceFallback;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程登录日志服务 OpenFeign 接口契约 (指向 system 内部接口)
 */
@FeignClient(contextId = "remoteLoginLogService", value = "wu-service-system", path = "/inner/login-log", fallbackFactory = RemoteLoginLogServiceFallback.class)
public interface RemoteLoginLogService {

    /**
     * 保存登录日志
     */
    @PostMapping("/save")
    R<Boolean> saveLoginLog(@RequestBody LoginLogDTO loginLogDTO);

    /**
     * 分页查询指定账号的登录记录
     */
    @GetMapping("/page-by-username")
    R<PageResult<LoginLogDTO>> pageByUsername(@RequestParam("username") String username,
                                              @RequestParam("pageNo") Integer pageNo,
                                              @RequestParam("pageSize") Integer pageSize);
}
