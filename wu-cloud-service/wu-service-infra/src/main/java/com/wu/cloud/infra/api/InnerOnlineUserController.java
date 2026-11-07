package com.wu.cloud.infra.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.service.monitor.OnlineUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线用户内部 RPC
 */
@Hidden
@RestController
@RequestMapping("/inner/online")
public class InnerOnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;

    @Operation(summary = "登录")
    @PostMapping("/record-login")
    public R<Boolean> recordLoginSession(@RequestBody RecordLoginSessionDTO dto) {
        onlineUserService.recordLoginSession(dto);
        return R.ok(true);
    }

    @Operation(summary = "新增")
    @PostMapping("/touch")
    public R<Boolean> touchLastAccess(@RequestParam("userId") Long userId) {
        onlineUserService.touchLastAccess(userId);
        return R.ok(true);
    }

    @Operation(summary = "获取统计信息")
    @GetMapping("/count")
    public R<Integer> countOnlineUsers() {
        return R.ok(onlineUserService.countOnlineUsers());
    }
}
