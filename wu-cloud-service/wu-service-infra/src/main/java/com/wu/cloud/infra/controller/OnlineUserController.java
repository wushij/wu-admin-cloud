package com.wu.cloud.infra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.wu.cloud.api.infra.dto.OnlineUserVO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.infra.service.monitor.OnlineUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 在线用户（与既有规范对齐 /monitor/online，网关映射为 /api/infra/online）
 */
@Tag(name = "在线用户")
@RestController
@RequestMapping("/api/infra/online")
public class OnlineUserController {

    @Resource
    private OnlineUserService onlineUserService;

    @RequiresPermissions("monitor:online:list")
    @Operation(summary = "在线用户列表")
    @GetMapping("/list")
    public R<List<OnlineUserVO>> list() {
        return R.ok(onlineUserService.listOnlineUsers());
    }

    @RequiresPermissions("monitor:online:list")
    @Operation(summary = "导出在线用户")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<OnlineUserVO> users = onlineUserService.listOnlineUsers();
        List<OnlineExportVO> rows = new ArrayList<>(users.size());
        for (OnlineUserVO u : users) {
            OnlineExportVO vo = new OnlineExportVO();
            vo.setUserId(String.valueOf(u.getUserId()));
            vo.setUsername(u.getLoginName() != null ? u.getLoginName() : "");
            vo.setIpaddr(u.getIpaddr() != null ? u.getIpaddr() : "");
            vo.setBrowser(u.getBrowser() != null ? u.getBrowser() : "");
            vo.setOs(u.getOs() != null ? u.getOs() : "");
            vo.setLoginTime(u.getLoginTime() != null ? u.getLoginTime() : "");
            rows.add(vo);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("在线用户", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), OnlineExportVO.class).sheet("在线用户").doWrite(rows);
    }

    @RequiresPermissions("monitor:online:forceLogout")
    @Operation(summary = "强退用户")
    @DeleteMapping("/{userId}")
    public R<Boolean> forceLogout(@PathVariable Long userId) {
        onlineUserService.forceLogout(userId);
        return R.ok(true);
    }

    @Data
    public static class OnlineExportVO {
        @ExcelProperty("用户ID")
        private String userId;
        @ExcelProperty("用户名")
        private String username;
        @ExcelProperty("IP")
        private String ipaddr;
        @ExcelProperty("浏览器")
        private String browser;
        @ExcelProperty("系统")
        private String os;
        @ExcelProperty("登录时间")
        private String loginTime;
    }
}
