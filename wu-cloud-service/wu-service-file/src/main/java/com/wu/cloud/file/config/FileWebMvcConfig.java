package com.wu.cloud.file.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 文件静态资源映射
 * <p>将存储层返回的 {@code /file/**} 访问路径映射到本地磁盘存储根目录，
 * 使上传后的文件 URL 可直接访问（此前无映射导致 URL 打不开）。</p>
 */
@Configuration
public class FileWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private FileStorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageProperties.getLocalBaseDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/file/**")
                .addResourceLocations(location);
    }
}
