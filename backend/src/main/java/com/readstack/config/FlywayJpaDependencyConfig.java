package com.readstack.config;

import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayJpaDependencyConfig extends EntityManagerFactoryDependsOnPostProcessor {
    public FlywayJpaDependencyConfig() {
        super("flyway");
    }
}
