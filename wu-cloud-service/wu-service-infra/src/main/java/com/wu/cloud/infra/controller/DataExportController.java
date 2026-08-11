package com.wu.cloud.infra.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.infra.entity.OperLogDO;
import com.wu.cloud.infra.mapper.OperLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导出 REST 控制器 (基于 EasyExcel)
 */
@RestController
@RequestMapping("/api/infra/export")
public class DataExportController {

    @Resource
    private OperLogMapper operLogMapper;

    @GetMapping("/demo")
    public void exportDemo(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("导出示例", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<DemoExportVO> data = new ArrayList<>();
        data.add(new DemoExportVO("1", "示例数据"));
        EasyExcel.write(response.getOutputStream(), DemoExportVO.class).sheet("示例").doWrite(data);
    }

    @GetMapping("/operlog")
    public void exportOperLog(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("操作日志", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<OperLogDO> list = operLogMapper.selectList(
                new LambdaQueryWrapper<OperLogDO>().orderByDesc(OperLogDO::getOperTime).last("LIMIT 1000"));
        List<OperLogExportVO> data = new ArrayList<>();
        for (OperLogDO log : list) {
            data.add(new OperLogExportVO(String.valueOf(log.getId()), log.getTitle(), log.getBusinessType(),
                    log.getMethod(), log.getRequestMethod(), log.getOperUrl(), log.getOperIp(),
                    log.getOperName(), log.getStatus(), log.getCostTime(), log.getOperTime()));
        }
        EasyExcel.write(response.getOutputStream(), OperLogExportVO.class).sheet("操作日志").doWrite(data);
    }

    /**
     * 示例导出 VO
     */
    @lombok.Data
    public static class DemoExportVO {
        @com.alibaba.excel.annotation.ExcelProperty("编号")
        private String id;
        @com.alibaba.excel.annotation.ExcelProperty("名称")
        private String name;

        public DemoExportVO(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * 操作日志导出 VO
     */
    @lombok.Data
    public static class OperLogExportVO {
        @com.alibaba.excel.annotation.ExcelProperty("ID")
        private String id;
        @com.alibaba.excel.annotation.ExcelProperty("操作模块")
        private String title;
        @com.alibaba.excel.annotation.ExcelProperty("业务类型")
        private String businessType;
        @com.alibaba.excel.annotation.ExcelProperty("方法")
        private String method;
        @com.alibaba.excel.annotation.ExcelProperty("请求方式")
        private String requestMethod;
        @com.alibaba.excel.annotation.ExcelProperty("请求URL")
        private String operUrl;
        @com.alibaba.excel.annotation.ExcelProperty("IP")
        private String operIp;
        @com.alibaba.excel.annotation.ExcelProperty("操作人")
        private String operName;
        @com.alibaba.excel.annotation.ExcelProperty("状态")
        private Integer status;
        @com.alibaba.excel.annotation.ExcelProperty("耗时(ms)")
        private Long costTime;
        @com.alibaba.excel.annotation.ExcelProperty("操作时间")
        private java.time.LocalDateTime operTime;

        public OperLogExportVO(String id, String title, String businessType, String method,
                               String requestMethod, String operUrl, String operIp, String operName,
                               Integer status, Long costTime, java.time.LocalDateTime operTime) {
            this.id = id;
            this.title = title;
            this.businessType = businessType;
            this.method = method;
            this.requestMethod = requestMethod;
            this.operUrl = operUrl;
            this.operIp = operIp;
            this.operName = operName;
            this.status = status;
            this.costTime = costTime;
            this.operTime = operTime;
        }
    }
}

