package com.finaldesign.lungnodule;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.finaldesign.lungnodule.mapper")
public class LungNoduleBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LungNoduleBackendApplication.class, args);
    }
}
