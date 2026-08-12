package com.wu.cloud.file.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("StaticFileSecurityFilter 单元测试")
class StaticFileSecurityFilterTest {

    private final Filter filter = new StaticFileSecurityFilter()
            .staticFileSecurityFilterRegistration()
            .getFilter();

    @Test
    @DisplayName("注册到 /file/* 且最高优先级")
    void registration_config() {
        FilterRegistrationBean<Filter> bean =
                new StaticFileSecurityFilter().staticFileSecurityFilterRegistration();
        assertTrue(bean.getUrlPatterns().contains("/file/*"));
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, bean.getOrder());
        assertNotNull(bean.getFilter());
    }

    @Test
    @DisplayName("所有响应写入 nosniff 与 CSP sandbox")
    void setsSecurityHeaders() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(req.getRequestURI()).thenReturn("/file/uploads/a.png");

        filter.doFilter(req, res, chain);

        verify(res).setHeader("X-Content-Type-Options", "nosniff");
        verify(res).setHeader("Content-Security-Policy", "default-src 'none'; sandbox");
        verify(res, never()).setHeader(eq("Content-Disposition"), anyString());
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("高危后缀强制 attachment 下载")
    void forceDownload_forDangerousExt() throws Exception {
        for (String uri : new String[]{
                "/file/uploads/x.html", "/file/a.SVG", "/file/app.js", "/file/style.css"
        }) {
            HttpServletRequest req = mock(HttpServletRequest.class);
            HttpServletResponse res = mock(HttpServletResponse.class);
            FilterChain chain = mock(FilterChain.class);
            when(req.getRequestURI()).thenReturn(uri);

            filter.doFilter(req, res, chain);

            verify(res).setHeader("Content-Disposition", "attachment");
            verify(chain).doFilter(req, res);
        }
    }
}
