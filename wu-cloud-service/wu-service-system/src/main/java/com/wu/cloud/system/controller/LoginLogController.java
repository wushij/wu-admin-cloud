package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.service.SysLoginLogService;
import com.wu.cloud.system.vo.LoginLogRespVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录日志 REST 控制器
 *
 * <p>对外查询端点。写入端点位于 {@code com.wu.cloud.system.api.InnerLoginLogController}，仅供认证服务 Feign 调用。</p>
 */
@Tag(name = "登录日志")
@RestController
@RequestMapping("/api/system/login-log")
public class LoginLogController {

    @Resource
    private SysLoginLogService sysLoginLogService;

    @RequiresPermissions("system:loginLog:query")
    @Operation(summary = "获取登录日志列表")
    @GetMapping("/list")
    public R<PageResult<LoginLogRespVO>> list(PageQuery pageQuery,
                                              @RequestParam(value = "username", required = false) String username,
                                              @RequestParam(value = "ipaddr", required = false) String ipaddr,
                                              @RequestParam(value = "status", required = false) Integer status) {
        Page<SysLoginLogDO> page = sysLoginLogService.getLoginLogPage(pageQuery, username, ipaddr, status);
        List<LoginLogRespVO> list = page.getRecords().stream().map(this::convert).toList();
        return R.ok(PageResult.of(list, page.getTotal()));
    }

    /**
     * 清空全部登录日志。
     *
     * <p>注意：该映射必须声明在 {@code /{id}} 之前，否则 "clear" 会被当作路径变量匹配。</p>
     */
    @RequiresPermissions("system:loginLog:clear")
    @Log(title = "登录日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "清空登录日志")
    @DeleteMapping("/clear")
    public R<Boolean> clear() {
        sysLoginLogService.clearLoginLog();
        return R.ok(true);
    }

    @RequiresPermissions("system:loginLog:delete")
    @Log(title = "登录日志", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除登录日志")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        sysLoginLogService.deleteLoginLog(id);
        return R.ok(true);
    }

    private LoginLogRespVO convert(SysLoginLogDO logDO) {
        LoginLogRespVO vo = new LoginLogRespVO();
        vo.setId(logDO.getId());
        vo.setUsername(logDO.getUsername());
        vo.setIpaddr(logDO.getIp());
        vo.setLoginLocation(logDO.getAddress());
        vo.setStatus(logDO.getStatus());
        vo.setMsg(logDO.getMessage());
        vo.setLoginTime(logDO.getLoginTime());
        vo.setBrowser(logDO.getBrowser());
        vo.setOs(logDO.getOs());
        return vo;
    }
}
