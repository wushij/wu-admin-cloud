package com.wu.cloud.codegen.config;

import com.wu.cloud.codegen.controller.CodegenController;
import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.codegen.service.impl.CodegenServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 代码生成器自动装配。
 *
 * <p>仅在存在数据源的 Web 服务中生效，可通过 {@code wu.codegen.enabled=false} 关闭。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "wu.codegen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CodegenAutoConfiguration {

    /**
     * 代码生成器专用 JdbcTemplate，避免与业务 JdbcTemplate 冲突
     */
    @Bean("codegenJdbcTemplate")
    @ConditionalOnMissingBean(name = "codegenJdbcTemplate")
    public JdbcTemplate codegenJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(GenTableReader.class)
    public GenTableReader genTableReader(JdbcTemplate codegenJdbcTemplate) {
        return new GenTableReader(codegenJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(CodegenService.class)
    public CodegenService codegenService() {
        return new CodegenServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(CodegenController.class)
    public CodegenController codegenController(GenTableReader genTableReader, CodegenService codegenService) {
        return new CodegenController(genTableReader, codegenService);
    }
}
