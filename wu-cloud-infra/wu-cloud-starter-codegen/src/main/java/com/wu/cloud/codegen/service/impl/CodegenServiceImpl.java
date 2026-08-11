package com.wu.cloud.codegen.service.impl;

import com.wu.cloud.codegen.domain.GenTable;
import com.wu.cloud.codegen.domain.GenTableColumn;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 基于 Velocity 的微服务代码生成实现。
 *
 * <p>生成产物遵循本项目的微服务分层约定：
 * entity / mapper / service / serviceImpl / controller / vo，
 * 开启 Feign 时额外生成 wu-cloud-api 契约与 Inner 控制器，
 * 并输出前端 api.ts、index.vue 与菜单 SQL。</p>
 */
@Slf4j
public class CodegenServiceImpl implements CodegenService {

    /** 模板根目录（classpath） */
    private static final String TEMPLATE_ROOT = "codegen/templates/";

    /** 后端模板：模板文件 -> 产物相对路径表达式（LinkedHashMap 保证产物顺序稳定） */
    private static final Map<String, String> BACKEND_TEMPLATES = new LinkedHashMap<>();

    /** Feign 相关模板，仅在 generateFeign=true 时生成 */
    private static final Map<String, String> FEIGN_TEMPLATES = new LinkedHashMap<>();

    /** 前端与 SQL 模板 */
    private static final Map<String, String> FRONTEND_TEMPLATES = new LinkedHashMap<>();

    static {
        BACKEND_TEMPLATES.put("entity.java.vm", "java/{package}/entity/{className}DO.java");
        BACKEND_TEMPLATES.put("mapper.java.vm", "java/{package}/mapper/{className}Mapper.java");
        BACKEND_TEMPLATES.put("service.java.vm", "java/{package}/service/{className}Service.java");
        BACKEND_TEMPLATES.put("serviceImpl.java.vm", "java/{package}/service/impl/{className}ServiceImpl.java");
        BACKEND_TEMPLATES.put("controller.java.vm", "java/{package}/controller/{className}Controller.java");
        BACKEND_TEMPLATES.put("pageReqVO.java.vm", "java/{package}/vo/{businessName}/{className}PageReqVO.java");
        BACKEND_TEMPLATES.put("saveReqVO.java.vm", "java/{package}/vo/{businessName}/{className}SaveReqVO.java");
        BACKEND_TEMPLATES.put("respVO.java.vm", "java/{package}/vo/{businessName}/{className}RespVO.java");

        FEIGN_TEMPLATES.put("dto.java.vm", "java/com/wu/cloud/api/{moduleName}/dto/{className}DTO.java");
        FEIGN_TEMPLATES.put("remoteService.java.vm",
                "java/com/wu/cloud/api/{moduleName}/feign/Remote{className}Service.java");
        FEIGN_TEMPLATES.put("innerController.java.vm", "java/{package}/controller/Inner{className}Controller.java");

        FRONTEND_TEMPLATES.put("api.ts.vm", "frontend/api/{moduleName}/{businessName}/index.ts");
        FRONTEND_TEMPLATES.put("index.vue.vm", "frontend/views/{moduleName}/{businessName}/index.vue");
        FRONTEND_TEMPLATES.put("menu.sql.vm", "sql/{businessName}_menu.sql");
    }

    private final VelocityEngine velocityEngine;

    public CodegenServiceImpl() {
        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "class");
        properties.setProperty("resource.loader.class.class", ClasspathResourceLoader.class.getName());
        properties.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.name());
        this.velocityEngine = new VelocityEngine(properties);
        this.velocityEngine.init();
    }

    @Override
    public Map<String, String> preview(GenTable table) {
        validate(table);
        VelocityContext context = buildContext(table);

        Map<String, String> result = new LinkedHashMap<>();
        BACKEND_TEMPLATES.forEach((tpl, path) -> render(context, table, tpl, path, result));
        if (table.isGenerateFeign()) {
            FEIGN_TEMPLATES.forEach((tpl, path) -> render(context, table, tpl, path, result));
        }
        FRONTEND_TEMPLATES.forEach((tpl, path) -> render(context, table, tpl, path, result));
        return result;
    }

    @Override
    public byte[] generateZip(GenTable table) {
        Map<String, String> files = preview(table);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("[代码生成] 打包 ZIP 失败 table={}", table.getTableName(), e);
            throw new BusinessException("代码打包失败：" + e.getMessage());
        }
    }

    /**
     * 渲染单个模板，模板缺失时跳过而非中断整体生成
     */
    private void render(VelocityContext context, GenTable table,
                        String templateName, String pathExpr, Map<String, String> result) {
        String templatePath = TEMPLATE_ROOT + templateName;
        try {
            Template template = velocityEngine.getTemplate(templatePath, StandardCharsets.UTF_8.name());
            StringWriter writer = new StringWriter();
            template.merge(context, writer);
            result.put(resolvePath(pathExpr, table), writer.toString());
        } catch (Exception e) {
            log.warn("[代码生成] 模板 {} 渲染失败，已跳过", templateName, e);
        }
    }

    /**
     * 将路径表达式中的占位符替换为实际值
     */
    private String resolvePath(String pathExpr, GenTable table) {
        return pathExpr
                .replace("{package}", table.getPackageName().replace('.', '/'))
                .replace("{className}", table.getClassName())
                .replace("{businessName}", table.getBusinessName())
                .replace("{moduleName}", table.getModuleName());
    }

    private VelocityContext buildContext(GenTable table) {
        VelocityContext context = new VelocityContext();
        context.put("table", table);
        context.put("columns", table.getColumns());
        context.put("pkColumn", table.getPkColumn());
        context.put("className", table.getClassName());
        context.put("classNameLower", uncapitalize(table.getClassName()));
        context.put("businessName", table.getBusinessName());
        context.put("moduleName", table.getModuleName());
        context.put("packageName", table.getPackageName());
        context.put("functionName", table.getFunctionName());
        context.put("tableName", table.getTableName());
        context.put("author", table.getAuthor());
        context.put("permissionPrefix", table.getPermissionPrefix());
        context.put("date", LocalDate.now().toString());
        context.put("importList", buildImports(table.getColumns()));
        return context;
    }

    /**
     * 汇总实体所需的额外 import（去重且有序）
     */
    private TreeSet<String> buildImports(List<GenTableColumn> columns) {
        TreeSet<String> imports = new TreeSet<>();
        for (GenTableColumn column : columns) {
            String javaType = column.getJavaType();
            // 仅全限定名类型需要 import，String/Long 等无需处理
            if (javaType != null && javaType.contains(".")) {
                imports.add(javaType);
            }
        }
        return imports;
    }

    private void validate(GenTable table) {
        if (table == null) {
            throw new BusinessException("生成配置不能为空");
        }
        if (isBlank(table.getTableName())) {
            throw new BusinessException("表名不能为空");
        }
        if (isBlank(table.getClassName())) {
            throw new BusinessException("类名不能为空");
        }
        if (isBlank(table.getPackageName())) {
            throw new BusinessException("包名不能为空");
        }
        if (isBlank(table.getModuleName())) {
            throw new BusinessException("模块名不能为空");
        }
        if (table.getColumns() == null || table.getColumns().isEmpty()) {
            throw new BusinessException("字段列表不能为空");
        }
        if (table.getPkColumn() == null) {
            throw new BusinessException("表 " + table.getTableName() + " 缺少主键，无法生成代码");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String uncapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }
}
