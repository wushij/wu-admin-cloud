package com.wu.cloud.common.xss;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

/**
 * XSS 过滤器：对非排除路径的请求进行 XSS 清洗
 */
public class XssFilter implements Filter {

    private final List<String> excludeUrls;

    public XssFilter(List<String> excludeUrls) {
        this.excludeUrls = excludeUrls == null ? List.of() : excludeUrls;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (isExcluded(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String contentType = httpRequest.getContentType();
        byte[] cachedBody = null;
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            cachedBody = httpRequest.getInputStream().readAllBytes();
        }

        chain.doFilter(new XssHttpServletRequestWrapper(httpRequest, cachedBody), response);
    }

    private boolean isExcluded(String uri) {
        if (uri == null) {
            return false;
        }
        for (String pattern : excludeUrls) {
            if (pattern.endsWith("/**")) {
                if (uri.startsWith(pattern.substring(0, pattern.length() - 3))) {
                    return true;
                }
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}
