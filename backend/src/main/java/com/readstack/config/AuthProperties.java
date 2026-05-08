package com.readstack.config;

import com.readstack.application.auth.AuthSettings;
import com.readstack.application.auth.AuthSessionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "readstack.auth")
public record AuthProperties(
        String accessTokenSecret,
        long accessTokenTtlSeconds,
        String refreshTokenHashSecret,
        long refreshTokenTtlDays,
        boolean cookieSecure,
        String cookieSameSite,
        boolean csrfEnabled,
        String initialUserEmail,
        String initialUserPassword
) implements AuthSettings, AuthSessionSettings {
}
