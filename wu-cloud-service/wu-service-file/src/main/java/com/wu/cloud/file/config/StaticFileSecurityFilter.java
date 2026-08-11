package com.wu.cloud.file.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 静态文件访问安全过滤器（P1-4 修复）。
 *
 * <p><b>问题</b>：{@code FileWebMvcConfig} 把整个上传目录映射到 {@code /file/**} 静态资源，
 * 浏览器访问时由 Spring 按扩展名推断 Content-Type 并<b>内联渲染</b>。
 * 若上传了 HTML/SVG 一类可执行脚本的文件，访问其 URL 就会在站点域下执行脚本，
 * 构成存储型 XSS，可直接盗取同域下的登录态。</p>
 *
 * <p><b>处理</b>：对 {@code /file/**} 的响应统一加固：</p>
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}：禁止浏览器 MIME 嗅探；</li>
 *   <li>{@code Content-Security-Policy: sandbox}：即便被当作 HTML 打开也禁用脚本执行；</li>
 *   <li>高危可渲染类型强制 {@code Content-Disposition: attachment}（下载而非内联展示）。</li>
 * </ul>
 *
 * <p>注：图片/PDF 等常见预览类型仍保持内联，不影响正常预览体验。</p>
 */
@Configuration
public class StaticFileSecurityFilter {

    /** 会被浏览器内联渲染、可能执行脚本的高危后缀，强制走下载 */
    private static final Set<String> FORCE_DOWNLOAD = Set.of(
            ".html", ".htm", ".xhtml", ".svg", ".xml", ".js", ".mjs", ".css", ".swf"
    );

    @Bean
    public FilterRegistrationBean<Filter> staticFileSecurityFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityFilter());
        registration.addUrlPatterns("/file/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("staticFileSecurityFilter");
        return registration;
    }

    private static class SecurityFilter implements Filter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            if (req instanceof HttpServletRequest request && res instanceof HttpServletResponse response) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Content-Security-Policy", "default-src 'none'; sandbox");

                String path = request.getRequestURI().toLowerCase(Locale.ROOT);
                int idx = path.lastIndexOf('.');
                String ext = idx >= 0 ? path.substring(idx) : "";
                if (FORCE_DOWNLOAD.contains(ext)) {
                    response.setHeader("Content-Disposition", "attachment");
                }
            }
            chain.doFilter(req, res);
        }
    }
}
