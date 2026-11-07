package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.entity.SysOperLogDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.OperLogMapper;
import com.wu.cloud.system.mapper.SysLoginLogMapper;
import com.wu.cloud.system.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统域数据导出（用户 / 登录日志 / 操作日志）
 */
@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/system/export")
public class DataExportController {

    private static final int MAX_ROWS = 5000;

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysLoginLogMapper sysLoginLogMapper;
    @Resource
    private OperLogMapper operLogMapper;

    @RequiresPermissions("system:user:list")
    @Operation(summary = "导出用户列表")
    @GetMapping("/user")
    public void exportUser(HttpServletResponse response,
                           @RequestParam(defaultValue = "xlsx") String format,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String mobile,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) Long deptId) throws IOException {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUserDO::getUsername, username);
        }
        if (StringUtils.hasText(mobile)) {
            wrapper.like(SysUserDO::getMobile, mobile);
        }
        if (status != null) {
            wrapper.eq(SysUserDO::getStatus, status);
        } else {
            wrapper.in(SysUserDO::getStatus, 0, 1);
        }
        if (deptId != null) {
            wrapper.eq(SysUserDO::getDeptId, deptId);
        }
        wrapper.orderByDesc(SysUserDO::getId).last("LIMIT " + MAX_ROWS);
        List<SysUserDO> users = sysUserMapper.selectList(wrapper);
        List<UserExportVO> rows = new ArrayList<>(users.size());
        for (SysUserDO u : users) {
            UserExportVO vo = new UserExportVO();
            vo.setId(String.valueOf(u.getId()));
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setMobile(u.getMobile());
            vo.setEmail(u.getEmail());
            vo.setStatus(u.getStatus());
            vo.setDeptId(u.getDeptId() == null ? "" : String.valueOf(u.getDeptId()));
            vo.setCreateTime(u.getCreateTime());
            rows.add(vo);
        }
        writeExcel(response, "用户列表", UserExportVO.class, rows);
    }

    @RequiresPermissions("system:loginLog:list")
    @Operation(summary = "导出登录日志")
    @GetMapping("/login-log")
    public void exportLoginLog(HttpServletResponse response,
                               @RequestParam(required = false) String username,
                               @RequestParam(required = false) String ipaddr,
                               @RequestParam(required = false) Integer status) throws IOException {
        LambdaQueryWrapper<SysLoginLogDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(SysLoginLogDO::getUsername, username);
        }
        if (StringUtils.hasText(ipaddr)) {
            wrapper.like(SysLoginLogDO::getIp, ipaddr);
        }
        if (status != null) {
            wrapper.eq(SysLoginLogDO::getStatus, status);
        }
        wrapper.orderByDesc(SysLoginLogDO::getId).last("LIMIT " + MAX_ROWS);
        List<SysLoginLogDO> logs = sysLoginLogMapper.selectList(wrapper);
        List<LoginLogExportVO> rows = new ArrayList<>(logs.size());
        for (SysLoginLogDO log : logs) {
            LoginLogExportVO vo = new LoginLogExportVO();
            vo.setId(String.valueOf(log.getId()));
            vo.setUsername(log.getUsername());
            vo.setIp(log.getIp());
            vo.setAddress(log.getAddress());
            vo.setBrowser(log.getBrowser());
            vo.setOs(log.getOs());
            vo.setStatus(log.getStatus());
            vo.setMsg(log.getMessage());
            vo.setLoginTime(log.getLoginTime());
            rows.add(vo);
        }
        writeExcel(response, "登录日志", LoginLogExportVO.class, rows);
    }

    @RequiresPermissions("system:operLog:list")
    @Operation(summary = "导出操作日志")
    @GetMapping("/oper-log")
    public void exportOperLog(HttpServletResponse response,
                              @RequestParam(required = false) String title,
                              @RequestParam(required = false) String operName,
                              @RequestParam(required = false) Integer status) throws IOException {
        LambdaQueryWrapper<SysOperLogDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            wrapper.like(SysOperLogDO::getTitle, title);
        }
        if (StringUtils.hasText(operName)) {
            wrapper.like(SysOperLogDO::getOperName, operName);
        }
        if (status != null) {
            wrapper.eq(SysOperLogDO::getStatus, status);
        }
        wrapper.orderByDesc(SysOperLogDO::getId).last("LIMIT " + MAX_ROWS);
        List<SysOperLogDO> logs = operLogMapper.selectList(wrapper);
        List<OperLogExportVO> rows = new ArrayList<>(logs.size());
        for (SysOperLogDO log : logs) {
            OperLogExportVO vo = new OperLogExportVO();
            vo.setId(String.valueOf(log.getId()));
            vo.setTitle(log.getTitle());
            vo.setBusinessType(log.getBusinessType() == null ? "" : String.valueOf(log.getBusinessType()));
            vo.setMethod(log.getMethod());
            vo.setRequestMethod(log.getRequestMethod());
            vo.setOperUrl(log.getOperUrl());
            vo.setOperIp(log.getOperIp());
            vo.setOperName(log.getOperName());
            vo.setStatus(log.getStatus());
            vo.setCreateTime(log.getOperTime());
            rows.add(vo);
        }
        writeExcel(response, "操作日志", OperLogExportVO.class, rows);
    }

    private <T> void writeExcel(HttpServletResponse response, String sheetName,
                                Class<T> clazz, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(sheetName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(data);
    }

    @Data
    public static class UserExportVO {
        @ExcelProperty("ID")
        private String id;
        @ExcelProperty("用户名")
        private String username;
        @ExcelProperty("昵称")
        private String nickname;
        @ExcelProperty("手机号")
        private String mobile;
        @ExcelProperty("邮箱")
        private String email;
        @ExcelProperty("状态")
        private Integer status;
        @ExcelProperty("部门ID")
        private String deptId;
        @ExcelProperty("创建时间")
        private LocalDateTime createTime;
    }

    @Data
    public static class LoginLogExportVO {
        @ExcelProperty("ID")
        private String id;
        @ExcelProperty("用户名")
        private String username;
        @ExcelProperty("IP")
        private String ip;
        @ExcelProperty("地点")
        private String address;
        @ExcelProperty("浏览器")
        private String browser;
        @ExcelProperty("系统")
        private String os;
        @ExcelProperty("状态")
        private Integer status;
        @ExcelProperty("消息")
        private String msg;
        @ExcelProperty("登录时间")
        private LocalDateTime loginTime;
    }

    @Data
    public static class OperLogExportVO {
        @ExcelProperty("ID")
        private String id;
        @ExcelProperty("模块")
        private String title;
        @ExcelProperty("业务类型")
        private String businessType;
        @ExcelProperty("方法")
        private String method;
        @ExcelProperty("请求方式")
        private String requestMethod;
        @ExcelProperty("URL")
        private String operUrl;
        @ExcelProperty("IP")
        private String operIp;
        @ExcelProperty("操作人")
        private String operName;
        @ExcelProperty("状态")
        private Integer status;
        @ExcelProperty("时间")
        private LocalDateTime createTime;
    }
}
