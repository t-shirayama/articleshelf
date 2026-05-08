package com.readstack;

import com.readstack.config.AuthProperties;
import com.readstack.config.AuthRateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties({AuthProperties.class, AuthRateLimitProperties.class})
public class ReadStackApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReadStackApplication.class, args);
    }
}
