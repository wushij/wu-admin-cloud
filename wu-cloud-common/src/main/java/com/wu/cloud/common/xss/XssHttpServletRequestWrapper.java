package com.wu.cloud.common.xss;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * XSS 请求包装器：对参数与 JSON 请求体做清洗
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public XssHttpServletRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
        super(request);
        this.cachedBody = cachedBody;
    }

    @Override
    public String getParameter(String name) {
        return XssUtils.clean(super.getParameter(name));
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] cleaned = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleaned[i] = XssUtils.clean(values[i]);
        }
        return cleaned;
    }

    @Override
    public String getHeader(String name) {
        return XssUtils.clean(super.getHeader(name));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBody == null) {
            return super.getInputStream();
        }
        String body = new String(cachedBody, StandardCharsets.UTF_8);
        byte[] cleaned = XssUtils.clean(body).getBytes(StandardCharsets.UTF_8);
        final InputStream delegate = new ByteArrayInputStream(cleaned);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                try {
                    return delegate.available() == 0;
                } catch (IOException e) {
                    return true;
                }
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 同步读取场景无需实现
            }

            @Override
            public int read() throws IOException {
                return delegate.read();
            }
        };
    }
}
