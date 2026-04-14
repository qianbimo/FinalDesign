package com.finaldesign.lungnodule.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lungNoduleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("肺结节智能分析系统 API")
                        .description("Spring Boot 3 + MyBatis-Plus 后端接口")
                        .version("v1.0.0")
                        .contact(new Contact().name("FinalDesign Team")))
                .externalDocs(new ExternalDocumentation()
                        .description("API Docs")
                        .url("http://localhost:8080/doc.html"));
    }
}
