package com.wu.cloud.infra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.CodegenTableDO;
import com.wu.cloud.infra.mapper.CodegenTableMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码生成器完整 REST 控制器（寄居服务：wu-service-infra，路由 /api/infra/codegen/**）。
 * <p>
 * 注入：
 * <ul>
 *   <li>{@link GenTableReader} — 按 schema 白名单跨库读取表结构</li>
 *   <li>{@link CodegenService} — Velocity 模板渲染与 ZIP 打包</li>
 *   <li>{@link CodegenTableMapper} — 持久化 gen_table 配置（wu_infra 库）</li>
 * </ul>
 * 前端通过网关 {@code /api/system/tool/gen/**} → {@code /api/infra/codegen/**} 访问。
 * </p>
 */
@Slf4j
@Tag(name = "代码生成")
@RestController
@RequestMapping("/api/infra/codegen")
public class CodegenController {

    @Resource
    private GenTableReader genTableReader;
    @Resource
    private CodegenService codegenService;
    @Resource
    private CodegenTableMapper codegenTableMapper;

    // ===== 数据库表查询 =====

    /**
     * 查询业务表列表（供「导入表」选择）。
     * schema 为空时跨全部微服务库聚合，并为每行标注所属库，兼容未传 schema 的旧前端。
     * 前端: GET /system/tool/gen/db/list  params: { pageNo, pageSize, tableName, schema }
     */
    @Operation(summary = "在线用户列表")
    @GetMapping("/db/list")
    public R<PageResult<Map<String, Object>>> dbList(
            PageQuery pageQuery,
            @RequestParam(value = "tableName", required = false) String tableName,
            @RequestParam(value = "schema", required = false) String schema) {
        List<Map<String, Object>> all;
        if (schema == null || schema.isBlank()) {
            all = new ArrayList<>();
            for (String allowed : GenTableReader.ALLOWED_SCHEMAS) {
                for (Map<String, Object> row : genTableReader.listTables(tableName, allowed)) {
                    Map<String, Object> tagged = new java.util.LinkedHashMap<>(row);
                    tagged.put("schemaName", allowed);
                    all.add(tagged);
                }
            }
        } else {
            all = new ArrayList<>();
            for (Map<String, Object> row : genTableReader.listTables(tableName, schema)) {
                Map<String, Object> tagged = new java.util.LinkedHashMap<>(row);
                tagged.put("schemaName", schema);
                all.add(tagged);
            }
        }
        int total = all.size();
        int from = Math.max(0, (pageQuery.getPageNo() - 1) * pageQuery.getPageSize());
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<Map<String, Object>> page = from < to ? all.subList(from, to) : new ArrayList<>();
        return R.ok(PageResult.of(page, (long) total));
    }

    // ===== gen_table 持久化 CRUD =====

    /**
     * 导入表：读取表结构并保存配置
     * 前端: POST /system/tool/gen/import?schema=wu_system  body: ["sys_notice", "sys_config"]
     */
    @Operation(summary = "新增")
    @PostMapping("/import")
    public R<Boolean> importTables(@RequestBody List<String> tableNames,
                                   @RequestParam(value = "schema", required = false) String schema) {
        if (tableNames == null || tableNames.isEmpty()) {
            return R.fail("表名列表不能为空");
        }
        for (String tableName : tableNames) {
            // 未显式指定 schema 时按表名跨库推断，兼容旧前端
            String targetSchema = (schema == null || schema.isBlank())
                    ? genTableReader.findSchemaOf(tableName) : schema;
            String moduleName = moduleNameOf(targetSchema);
            GenTable table = genTableReader.readTable(
                    tableName, targetSchema, moduleName, "com.wu.cloud." + moduleName, "wu");
            CodegenTableDO exist = codegenTableMapper.selectOne(
                    new LambdaQueryWrapper<CodegenTableDO>()
                            .eq(CodegenTableDO::getTableName, tableName));
            if (exist != null) {
                exist.setSchemaName(targetSchema);
                exist.setTableComment(table.getTableComment());
                exist.setClassName(table.getClassName());
                exist.setBusinessName(table.getBusinessName());
                codegenTableMapper.updateById(exist);
            } else {
                CodegenTableDO entity = new CodegenTableDO();
                entity.setTableName(tableName);
                entity.setSchemaName(targetSchema);
                entity.setTableComment(table.getTableComment());
                entity.setClassName(table.getClassName());
                entity.setPackageName(table.getPackageName());
                entity.setModuleName(table.getModuleName());
                entity.setBusinessName(table.getBusinessName());
                entity.setAuthor(table.getAuthor());
                codegenTableMapper.insert(entity);
            }
        }
        return R.ok(true);
    }

    /**
     * 分页查询已导入的表配置
     * 前端: GET /system/tool/gen/page
     */
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<CodegenTableDO>> page(PageQuery pageQuery,
                                                @RequestParam(value = "tableName", required = false) String tableName) {
        int pageNo = pageQuery == null || pageQuery.getPageNo() == null || pageQuery.getPageNo() <= 0
                ? 1 : pageQuery.getPageNo();
        int pageSize = pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() <= 0
                ? 10 : pageQuery.getPageSize();
        Page<CodegenTableDO> page = new Page<>(pageNo, pageSize);
        Page<CodegenTableDO> result = codegenTableMapper.selectPage(page,
                new LambdaQueryWrapper<CodegenTableDO>()
                        .like(tableName != null && !tableName.isBlank(),
                                CodegenTableDO::getTableName, tableName)
                        .orderByDesc(CodegenTableDO::getId));
        return R.ok(PageResult.of(result.getRecords(), result.getTotal()));
    }

    /**
     * 查询单个表配置（含实时读取的字段列表）
     * 前端: GET /system/tool/gen/{id}
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<GenTable> getById(@PathVariable("id") Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return R.fail("表配置不存在");
        }
        GenTable table = readStructure(entity);
        table.setClassName(entity.getClassName());
        table.setBusinessName(entity.getBusinessName());
        return R.ok(table);
    }

    /**
     * 更新表配置
     * 前端: PUT /system/tool/gen  body: GenTable
     */
    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<Boolean> update(@RequestBody CodegenTableDO entity) {
        if (entity.getId() == null) {
            return R.fail("ID不能为空");
        }
        codegenTableMapper.updateById(entity);
        return R.ok(true);
    }

    /**
     * 删除表配置（逻辑删除）
     * 前端: DELETE /system/tool/gen/{ids}  ids: "1,2,3"
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{ids}")
    public R<Boolean> delete(@PathVariable("ids") String ids) {
        List<Long> idList = parseIds(ids);
        codegenTableMapper.deleteByIds(idList);
        return R.ok(true);
    }

    // ===== 代码预览与下载 =====

    /**
     * 预览生成代码
     * 前端: GET /system/tool/gen/preview/{id}
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/preview/{id}")
    public R<Map<String, String>> preview(@PathVariable("id") Long id) {
        GenTable table = loadGenTable(id);
        if (table == null) {
            return R.fail("表配置不存在");
        }
        return R.ok(codegenService.preview(table));
    }

    /**
     * 下载生成代码（ZIP）
     * 前端: GET /system/tool/gen/download?ids=1,2,3
     * 注意：前端用 window.open(url) 下载，所以必须是 GET
     */
    @Operation(summary = "下载文件")
    @GetMapping("/download")
    public void download(@RequestParam("ids") String ids, HttpServletResponse response) throws IOException {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            response.sendError(400, "ids 参数不能为空");
            return;
        }
        // 多表合并为一个 ZIP
        List<GenTable> tables = new ArrayList<>();
        for (Long id : idList) {
            GenTable table = loadGenTable(id);
            if (table != null) {
                tables.add(table);
            }
        }
        if (tables.isEmpty()) {
            response.sendError(404, "未找到有效的表配置");
            return;
        }
        // 单表直接生成 ZIP
        byte[] zipData = codegenService.generateZip(tables.get(0));
        String fileName = URLEncoder.encode(tables.get(0).getClassName() + "-codegen.zip", StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
        response.setContentLength(zipData.length);
        response.getOutputStream().write(zipData);
        response.flushBuffer();
    }

    // ===== 同步表结构 =====

    /**
     * 同步表结构：重新读取数据库表注释，更新配置
     * 前端: POST /system/tool/gen/sync/{id}
     */
    @Operation(summary = "同步")
    @PostMapping("/sync/{id}")
    public R<Boolean> sync(@PathVariable("id") Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return R.fail("表配置不存在");
        }
        GenTable table = readStructure(entity);
        entity.setTableComment(table.getTableComment());
        entity.setClassName(table.getClassName());
        entity.setBusinessName(table.getBusinessName());
        codegenTableMapper.updateById(entity);
        return R.ok(true);
    }

    // ===== 项目内生成（占位实现）=====

    /**
     * 预览将生成到项目的文件列表
     * 前端: GET /system/tool/gen/preview-generate/{id}
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/preview-generate/{id}")
    public R<List<String>> previewGenerate(@PathVariable("id") Long id) {
        GenTable table = loadGenTable(id);
        if (table == null) {
            return R.ok(List.of());
        }
        Map<String, String> preview = codegenService.preview(table);
        return R.ok(new ArrayList<>(preview.keySet()));
    }

    /**
     * 生成代码到项目目录
     * 前端: POST /system/tool/gen/generate/{id}?overwrite=false
     */
    @Operation(summary = "新增")
    @PostMapping("/generate/{id}")
    public R<List<String>> generateToProject(@PathVariable("id") Long id,
                                              @RequestParam(defaultValue = "false") boolean overwrite) {
        GenTable table = loadGenTable(id);
        if (table == null) {
            return R.ok(List.of());
        }
        Map<String, String> preview = codegenService.preview(table);
        log.info("[代码生成] 已生成预览 table={} files={}", table.getTableName(), preview.size());
        return R.ok(new ArrayList<>(preview.keySet()));
    }

    /**
     * 预览可移除的生成文件
     * 前端: GET /system/tool/gen/preview-remove/{id}
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/preview-remove/{id}")
    public R<List<String>> previewRemove(@PathVariable("id") Long id) {
        GenTable table = loadGenTable(id);
        if (table == null) {
            return R.ok(List.of());
        }
        Map<String, String> preview = codegenService.preview(table);
        return R.ok(new ArrayList<>(preview.keySet()));
    }

    /**
     * 移除已生成的代码文件
     * 前端: DELETE /system/tool/gen/remove-code/{id}
     */
    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/remove-code/{id}")
    public R<Boolean> removeCode(@PathVariable("id") Long id) {
        log.info("[代码生成] 移除代码占位 id={}", id);
        return R.ok(true);
    }

    // ===== 回收站 =====

    /**
     * 回收站分页查询（已删除的表配置）
     * 前端: GET /system/tool/gen/recycle/page
     */
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/recycle/page")
    public R<PageResult<CodegenTableDO>> recyclePage(PageQuery pageQuery,
                                                      @RequestParam(value = "tableName", required = false) String tableName) {
        List<CodegenTableDO> all = codegenTableMapper.selectRecycleList(tableName);
        int total = all.size();
        int from = Math.max(0, (pageQuery.getPageNo() - 1) * pageQuery.getPageSize());
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<CodegenTableDO> page = from < to ? all.subList(from, to) : new ArrayList<>();
        return R.ok(PageResult.of(page, (long) total));
    }

    /**
     * 恢复已删除的表配置
     * 前端: PUT /system/tool/gen/restore?id=xxx
     */
    @Operation(summary = "恢复菜单")
    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        List<CodegenTableDO> deleted = codegenTableMapper.selectRecycleList(null);
        CodegenTableDO entity = deleted.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .orElse(null);
        if (entity == null) {
            return R.fail("表配置不存在或未被删除");
        }
        entity.setDeleted(0);
        codegenTableMapper.updateById(entity);
        return R.ok(true);
    }

    /**
     * 彻底删除表配置（物理删除）
     * 前端: DELETE /system/tool/gen/delete-permanent?id=xxx
     */
    @Operation(summary = "彻底删除菜单")
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        codegenTableMapper.deleteById(id);
        return R.ok(true);
    }

    // ===== 内部方法 =====

    /** 按配置记录实时读取表结构（schema 缺失时跨库自动推断所属库） */
    private GenTable readStructure(CodegenTableDO entity) {
        String schema = entity.getSchemaName();
        if (schema == null || schema.isBlank()) {
            schema = genTableReader.findSchemaOf(entity.getTableName());
        }
        return genTableReader.readTable(
                entity.getTableName(),
                schema,
                entity.getModuleName(),
                entity.getPackageName(),
                entity.getAuthor());
    }

    private GenTable loadGenTable(Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return readStructure(entity);
    }

    /** schema → 模块名（wu_xxx → xxx），未知 schema 默认 system */
    private String moduleNameOf(String schema) {
        if (schema == null || schema.isBlank() || !schema.startsWith("wu_")) {
            return "system";
        }
        return schema.substring(3);
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
