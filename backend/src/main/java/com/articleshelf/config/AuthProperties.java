package com.articleshelf.config;

import com.articleshelf.application.auth.AuthSettings;
import com.articleshelf.application.auth.AuthSessionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "articleshelf.auth")
public record AuthProperties(
        String accessTokenSecret,
        long accessTokenTtlSeconds,
        String refreshTokenHashSecret,
        long refreshTokenTtlDays,
        boolean cookieSecure,
        String cookieSameSite,
        boolean csrfEnabled,
        boolean initialUserEnabled,
        String initialUsername,
        String initialUserPassword
) implements AuthSettings, AuthSessionSettings {
}
