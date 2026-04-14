package com.finaldesign.lungnodule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String baseUrl;
    private String predictPath;
    private Integer timeoutSeconds;
    private Boolean mockEnabled;
}
