package com.atlasmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AtlasMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtlasMonitorApplication.class, args);
    }
}
