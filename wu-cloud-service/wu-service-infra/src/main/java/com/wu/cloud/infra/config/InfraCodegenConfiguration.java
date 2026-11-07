package com.wu.cloud.infra.config;

import com.wu.cloud.codegen.service.CodegenService;
import com.wu.cloud.codegen.service.GenTableReader;
import com.wu.cloud.codegen.service.impl.CodegenServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 代码生成器基础设施装配（寄居服务：wu-service-infra）。
 * <p>
 * 关闭 starter 的自动装配（wu.codegen.enabled=false）后由本类显式注册组件，
 * 避免 starter 内置的精简控制器与本服务的完整控制器产生重复路由映射。
 * gen_table 配置表随代码生成器一并落在 wu_infra 库。
 * </p>
 */
@Configuration
public class InfraCodegenConfiguration {

    @Bean("codegenJdbcTemplate")
    public JdbcTemplate codegenJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public GenTableReader genTableReader(JdbcTemplate codegenJdbcTemplate) {
        return new GenTableReader(codegenJdbcTemplate);
    }

    @Bean
    public CodegenService codegenService() {
        return new CodegenServiceImpl();
    }
}
