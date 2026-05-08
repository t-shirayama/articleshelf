package com.articleshelf.config;

import com.articleshelf.application.auth.AuthRateLimitSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "articleshelf.auth-rate-limit")
public record AuthRateLimitProperties(
        boolean enabled,
        int loginCapacity,
        long loginWindowSeconds,
        int registerCapacity,
        long registerWindowSeconds
) implements AuthRateLimitSettings {
}
