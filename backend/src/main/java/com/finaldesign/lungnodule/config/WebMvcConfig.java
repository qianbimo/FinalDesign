package com.finaldesign.lungnodule.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public WebMvcConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = storageProperties.getBasePath().replace("\\", "/");
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }
        String accessPrefix = storageProperties.getAccessPrefix();
        if (!accessPrefix.endsWith("/**")) {
            accessPrefix = accessPrefix + "/**";
        }
        registry.addResourceHandler(accessPrefix).addResourceLocations("file:" + basePath);
    }
}
