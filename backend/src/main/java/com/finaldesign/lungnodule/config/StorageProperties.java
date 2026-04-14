package com.finaldesign.lungnodule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private String basePath;
    private String accessPrefix;
    private Integer maxSizeMb;
    private List<String> allowedExtensions;
}
