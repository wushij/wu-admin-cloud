package com.wu.cloud.codegen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>
 * 受 {@code wu.codegen.enabled} 开关控制（默认开启）。当寄居服务（如 wu-service-infra）
 * 需要提供自己更完整的控制器时，将其设置为 {@code false} 即可同时关闭
 * {@link com.wu.cloud.codegen.config.CodegenAutoConfiguration} 的自动装配与本控制器的组件扫描，
 * 避免同名 Bean 与重复路由映射。
 * </p>
 */
@Slf4j
@Tag(name = "代码生成")
@RestController
@ConditionalOnProperty(prefix = "wu.codegen", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    @RequiresPermissions("tool:gen:list")
    @Operation(summary = "获取列表")
    @GetMapping("/db-tables")
    public R<List<Map<String, Object>>> listDbTables(
            @RequestParam(value = "tableName", required = false) String tableName) {
        return R.ok(genTableReader.listTables(tableName, null));
    }

    /**
     * 读取表结构，返回可编辑的默认生成配置
     */
    @RequiresPermissions("tool:gen:query")
    @Operation(summary = "获取详情")
    @GetMapping("/table-info")
    public R<GenTable> getTableInfo(
            @RequestParam("tableName") String tableName,
            @RequestParam(value = "moduleName", defaultValue = "system") String moduleName,
            @RequestParam(value = "packageName", defaultValue = "com.wu.cloud.system") String packageName,
            @RequestParam(value = "author", defaultValue = "wu") String author) {
        return R.ok(genTableReader.readTable(tableName, null, moduleName, packageName, author));
    }

    /**
     * 预览生成结果
     */
    @RequiresPermissions("tool:gen:preview")
    @Operation(summary = "预览生成结果")
    @PostMapping("/preview")
    public R<Map<String, String>> preview(@RequestBody GenTable table) {
        return R.ok(codegenService.preview(table));
    }

    /**
     * 下载生成的代码压缩包
     */
    @RequiresPermissions("tool:gen:preview")
    @Operation(summary = "下载文件")
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
