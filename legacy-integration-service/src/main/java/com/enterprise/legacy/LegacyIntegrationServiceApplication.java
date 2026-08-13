package com.enterprise.legacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.enterprise.legacy", "com.enterprise.events"})
public class LegacyIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyIntegrationServiceApplication.class, args);
    }
}
