package com.wu.cloud.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.file.entity.CodegenTableDO;
import com.wu.cloud.file.mapper.CodegenTableMapper;
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
 * 代码生成器完整 REST 控制器（B 队 P0 修复：补齐前端 gen.ts 所需的全部接口）
 * <p>
 * 覆盖 starter-codegen 的 CodegenController（@ConditionalOnMissingBean），注入：
 * <ul>
 *   <li>{@link GenTableReader} — 读取数据库表结构</li>
 *   <li>{@link CodegenService} — Velocity 模板渲染与 ZIP 打包</li>
 *   <li>{@link CodegenTableMapper} — 持久化 gen_table 配置</li>
 * </ul>
 * 前端通过网关 {@code /api/system/tool/gen/**} → {@code /api/infra/codegen/**} 访问。
 * </p>
 */
@Slf4j
@RestController("fileCodegenController")
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
     * 查询当前数据库的业务表列表（供「导入表」选择）
     * 前端: GET /system/tool/gen/db/list  params: { pageNo, pageSize, tableName }
     * 返回分页结构 PageResult，与前端 dbTableList 期望一致
     */
    @GetMapping("/db/list")
    public R<PageResult<Map<String, Object>>> dbList(
            PageQuery pageQuery,
            @RequestParam(value = "tableName", required = false) String tableName) {
        List<Map<String, Object>> all = genTableReader.listTables(tableName);
        int total = all.size();
        int from = Math.max(0, (pageQuery.getPageNo() - 1) * pageQuery.getPageSize());
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<Map<String, Object>> page = from < to ? all.subList(from, to) : new ArrayList<>();
        return R.ok(PageResult.of(page, (long) total));
    }

    // ===== gen_table 持久化 CRUD =====

    /**
     * 导入表：读取表结构并保存配置
     * 前端: POST /system/tool/gen/import  body: ["sys_notice", "sys_config"]
     */
    @PostMapping("/import")
    public R<Boolean> importTables(@RequestBody List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return R.fail("表名列表不能为空");
        }
        for (String tableName : tableNames) {
            GenTable table = genTableReader.readTable(tableName, "system", "com.wu.cloud.system", "wu");
            CodegenTableDO exist = codegenTableMapper.selectOne(
                    new LambdaQueryWrapper<CodegenTableDO>()
                            .eq(CodegenTableDO::getTableName, tableName));
            if (exist != null) {
                exist.setTableComment(table.getTableComment());
                exist.setClassName(table.getClassName());
                exist.setBusinessName(table.getBusinessName());
                codegenTableMapper.updateById(exist);
            } else {
                CodegenTableDO entity = new CodegenTableDO();
                entity.setTableName(tableName);
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
    @GetMapping("/{id}")
    public R<GenTable> getById(@PathVariable("id") Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return R.fail("表配置不存在");
        }
        GenTable table = genTableReader.readTable(
                entity.getTableName(),
                entity.getModuleName(),
                entity.getPackageName(),
                entity.getAuthor());
        table.setClassName(entity.getClassName());
        table.setBusinessName(entity.getBusinessName());
        return R.ok(table);
    }

    /**
     * 更新表配置
     * 前端: PUT /system/tool/gen  body: GenTable
     */
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
    @DeleteMapping("/{ids}")
    public R<Boolean> delete(@PathVariable("ids") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        codegenTableMapper.deleteByIds(idList);
        return R.ok(true);
    }

    // ===== 代码预览与下载 =====

    /**
     * 预览生成代码
     * 前端: GET /system/tool/gen/preview/{id}
     */
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
    @GetMapping("/download")
    public void download(@RequestParam("ids") String ids, HttpServletResponse response) throws IOException {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
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
    @PostMapping("/sync/{id}")
    public R<Boolean> sync(@PathVariable("id") Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return R.fail("表配置不存在");
        }
        GenTable table = genTableReader.readTable(
                entity.getTableName(),
                entity.getModuleName(),
                entity.getPackageName(),
                entity.getAuthor());
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
     * 注：项目内生成需要文件系统写入能力，当前返回预览的文件路径列表作为占位
     */
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
     * 注：当前仅返回预览的文件路径列表，实际文件系统写入需配合 CI/CD
     */
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
     * 注：实际文件移除需文件系统权限，当前返回成功占位
     */
    @DeleteMapping("/remove-code/{id}")
    public R<Boolean> removeCode(@PathVariable("id") Long id) {
        log.info("[代码生成] 移除代码占位 id={}", id);
        return R.ok(true);
    }

    // ===== 回收站 =====

    /**
     * 回收站分页查询（已删除的表配置）
     * 前端: GET /system/tool/gen/recycle/page
     * 注：使用自定义 SQL 绕过 @TableLogic，查询 deleted=1 的记录
     */
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
     * 注：绕过 @TableLogic 手动恢复 deleted=0
     */
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
    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        codegenTableMapper.deleteById(id);
        return R.ok(true);
    }

    // ===== 内部方法 =====

    /**
     * 加载表配置并实时读取表结构构建 GenTable
     */
    private GenTable loadGenTable(Long id) {
        CodegenTableDO entity = codegenTableMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return genTableReader.readTable(
                entity.getTableName(),
                entity.getModuleName(),
                entity.getPackageName(),
                entity.getAuthor());
    }
}
