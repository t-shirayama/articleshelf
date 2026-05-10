package com.articleshelf.config;

import com.articleshelf.application.auth.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

@Configuration
public class SystemProviderConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator idGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
