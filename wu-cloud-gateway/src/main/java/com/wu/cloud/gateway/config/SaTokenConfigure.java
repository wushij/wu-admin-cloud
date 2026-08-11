package com.wu.cloud.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sa-Token 网关鉴权配置（官方 WebFlux 适配）
 */
@Configuration
public class SaTokenConfigure {

    private final AuthWhitelist authWhitelist;

    public SaTokenConfigure(AuthWhitelist authWhitelist) {
        this.authWhitelist = authWhitelist;
    }

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        SaReactorFilter filter = new SaReactorFilter();
        List<String> list = authWhitelist.getWhitelist();
        if (list != null && !list.isEmpty()) {
            filter.addExclude(list.toArray(new String[0]));
        }
        filter.setAuth(r -> StpUtil.checkLogin());
        return filter;
    }
}
