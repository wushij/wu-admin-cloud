package com.wu.cloud.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.service.SysLoginLogService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录日志内部 RPC 端点（仅供认证服务 OpenFeign 调用，由网关 403 黑名单保护）
 */
@RestController
@RequestMapping("/inner/login-log")
public class InnerLoginLogController {

    @Resource
    private SysLoginLogService sysLoginLogService;

    @PostMapping("/save")
    public R<Boolean> saveLoginLog(@RequestBody LoginLogDTO logDTO) {
        SysLoginLogDO logDO = new SysLoginLogDO();
        logDO.setUserId(logDTO.getUserId());
        logDO.setUsername(logDTO.getUsername());
        logDO.setIp(logDTO.getIp());
        logDO.setAddress(logDTO.getAddress());
        logDO.setBrowser(logDTO.getBrowser());
        logDO.setOs(logDTO.getOs());
        logDO.setStatus(logDTO.getStatus());
        logDO.setMessage(logDTO.getMessage());
        logDO.setLoginTime(logDTO.getLoginTime());
        sysLoginLogService.recordLoginLog(logDO);
        return R.ok(true);
    }

    @GetMapping("/page-by-username")
    public R<PageResult<LoginLogDTO>> pageByUsername(@RequestParam("username") String username,
                                                     @RequestParam("pageNo") Integer pageNo,
                                                     @RequestParam("pageSize") Integer pageSize) {
        Page<SysLoginLogDO> page = sysLoginLogService.pageByUsername(username, pageNo, pageSize);
        List<LoginLogDTO> list = page.getRecords().stream().map(this::convert).toList();
        return R.ok(PageResult.of(list, page.getTotal()));
    }

    private LoginLogDTO convert(SysLoginLogDO logDO) {
        LoginLogDTO dto = new LoginLogDTO();
        dto.setId(logDO.getId());
        dto.setUserId(logDO.getUserId());
        dto.setUsername(logDO.getUsername());
        dto.setIp(logDO.getIp());
        dto.setAddress(logDO.getAddress());
        dto.setBrowser(logDO.getBrowser());
        dto.setOs(logDO.getOs());
        dto.setStatus(logDO.getStatus());
        dto.setMessage(logDO.getMessage());
        dto.setLoginTime(logDO.getLoginTime());
        return dto;
    }
}
