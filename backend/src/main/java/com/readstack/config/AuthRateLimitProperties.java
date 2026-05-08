package com.readstack.config;

import com.readstack.application.auth.AuthRateLimitSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "readstack.auth-rate-limit")
public record AuthRateLimitProperties(
        boolean enabled,
        int loginCapacity,
        long loginWindowSeconds,
        int registerCapacity,
        long registerWindowSeconds
) implements AuthRateLimitSettings {
}
