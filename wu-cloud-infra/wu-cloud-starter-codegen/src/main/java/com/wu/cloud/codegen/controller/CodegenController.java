package com.wu.cloud.codegen.controller;

import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 代码生成器 API
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/codegen")
public class CodegenController {

    private final GenTableReader genTableReader;
    private final CodegenService codegenService;

    public CodegenController(GenTableReader genTableReader, CodegenService codegenService) {
        this.genTableReader = genTableReader;
        this.codegenService = codegenService;
    }

    /**
     * 查询当前数据库的业务表列表
     */
    @RequiresPermissions("infra:codegen:list")
    @GetMapping("/db-tables")
    public R<List<Map<String, Object>>> listDbTables(
            @RequestParam(value = "tableName", required = false) String tableName) {
        return R.ok(genTableReader.listTables(tableName));
    }

    /**
     * 读取表结构，返回可编辑的默认生成配置
     */
    @RequiresPermissions("infra:codegen:query")
    @GetMapping("/table-info")
    public R<GenTable> getTableInfo(
            @RequestParam("tableName") String tableName,
            @RequestParam(value = "moduleName", defaultValue = "system") String moduleName,
            @RequestParam(value = "packageName", defaultValue = "com.wu.cloud.system") String packageName,
            @RequestParam(value = "author", defaultValue = "wu") String author) {
        return R.ok(genTableReader.readTable(tableName, moduleName, packageName, author));
    }

    /**
     * 预览生成结果
     */
    @RequiresPermissions("infra:codegen:preview")
    @PostMapping("/preview")
    public R<Map<String, String>> preview(@RequestBody GenTable table) {
        return R.ok(codegenService.preview(table));
    }

    /**
     * 下载生成的代码压缩包
     */
    @RequiresPermissions("infra:codegen:download")
    @PostMapping("/download")
    public void download(@RequestBody GenTable table, HttpServletResponse response) throws IOException {
        byte[] data = codegenService.generateZip(table);

        String fileName = URLEncoder.encode(table.getClassName() + "-codegen.zip", StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.flushBuffer();
    }
}
