package com.city.manhole.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源映射：/uploads/** 指向项目根目录 uploads/，不指向用户主目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        String abs = dir.getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + abs + File.separator)
                .setCachePeriod(3600);
    }
}
