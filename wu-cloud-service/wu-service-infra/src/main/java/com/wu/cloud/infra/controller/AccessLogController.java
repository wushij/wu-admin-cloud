package com.wu.cloud.infra.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.infra.entity.AccessLogDO;
import com.wu.cloud.infra.entity.ApiAccessLogDO;
import com.wu.cloud.infra.service.AccessLogService;
import com.wu.cloud.infra.service.ApiAccessLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * API 访问日志 REST 控制器。
 * B 队修复：网关 compat-monitor-access 将前端 /system/monitor/api-access/** 重写为
 * /api/infra/access-log/**，此处补齐 /page 与 /statistics（对应前端 getApiAccessPage/getApiAccessStatistics）。
 */
@RestController
@RequestMapping("/api/infra/access-log")
public class AccessLogController {

    private static final int MAX_ROWS = 5000;

    @Resource
    private AccessLogService accessLogService;
    @Resource
    private ApiAccessLogService apiAccessLogService;

    @PostMapping("/save")
    public R<Boolean> save(@RequestBody AccessLogDO accessLogDO) {
        accessLogService.saveAccessLog(accessLogDO);
        return R.ok(true);
    }

    /** B 队修复：分页查询 API 访问日志（前端 getApiAccessPage） */
    @GetMapping("/page")
    public R<PageResult<ApiAccessLogDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(apiAccessLogService.page(pageNo, pageSize, userId, apiPath, method, success, startTime, endTime));
    }

    /** B 队修复：统计 API 访问日志（前端 getApiAccessStatistics） */
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LocalDateTime begin = startTime;
        LocalDateTime finish = endTime;
        if (begin == null) {
            LocalDate s = startDate != null ? startDate : LocalDate.now().minusDays(6);
            begin = s.atStartOfDay();
        }
        if (finish == null) {
            LocalDate e = endDate != null ? endDate : LocalDate.now();
            finish = e.atTime(23, 59, 59);
        }
        return R.ok(apiAccessLogService.getStatistics(begin, finish));
    }

    /** 导出 API 访问日志（前端 /system/monitor/api-access/export） */
    @RequiresPermissions("monitor:apiAccess:query")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(defaultValue = "xlsx") String format,
                       @RequestParam(defaultValue = "filtered") String scope,
                       @RequestParam(required = false) Integer pageNo,
                       @RequestParam(required = false) Integer pageSize,
                       @RequestParam(required = false) Long userId,
                       @RequestParam(required = false) String apiPath,
                       @RequestParam(required = false) String method,
                       @RequestParam(required = false) Integer success,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime)
            throws IOException {
        int exportPageNo = "page".equalsIgnoreCase(scope) && pageNo != null ? pageNo : 1;
        int exportPageSize = "page".equalsIgnoreCase(scope) && pageSize != null ? pageSize : MAX_ROWS;
        if (!"page".equalsIgnoreCase(scope)) {
            exportPageSize = MAX_ROWS;
        }
        PageResult<ApiAccessLogDO> page = apiAccessLogService.page(
                exportPageNo, exportPageSize, userId, apiPath, method, success, startTime, endTime);
        List<ApiAccessExportVO> rows = new ArrayList<>();
        if (page.getList() != null) {
            for (ApiAccessLogDO log : page.getList()) {
                ApiAccessExportVO vo = new ApiAccessExportVO();
                vo.setId(log.getId() == null ? "" : String.valueOf(log.getId()));
                vo.setUserId(log.getUserId() == null ? "" : String.valueOf(log.getUserId()));
                vo.setUsername(log.getUsername());
                vo.setApiPath(log.getApiPath());
                vo.setMethod(log.getMethod());
                vo.setSuccess(log.getSuccess());
                vo.setCostTime(log.getCostTime());
                vo.setIp(log.getIp());
                vo.setStartTime(log.getStartTime());
                rows.add(vo);
            }
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("API访问日志", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ApiAccessExportVO.class).sheet("API访问日志").doWrite(rows);
    }

    @Data
    private static class ApiAccessExportVO {
        @ExcelProperty("ID")
        private String id;
        @ExcelProperty("用户ID")
        private String userId;
        @ExcelProperty("用户名")
        private String username;
        @ExcelProperty("接口路径")
        private String apiPath;
        @ExcelProperty("请求方法")
        private String method;
        @ExcelProperty("是否成功")
        private Integer success;
        @ExcelProperty("耗时(ms)")
        private Long costTime;
        @ExcelProperty("IP")
        private String ip;
        @ExcelProperty("访问时间")
        private LocalDateTime startTime;
    }
}