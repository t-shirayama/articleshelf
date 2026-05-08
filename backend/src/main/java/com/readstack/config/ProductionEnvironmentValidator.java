package com.readstack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

@Component
public class ProductionEnvironmentValidator {
    private final Environment environment;
    private final AuthProperties authProperties;
    private final String frontendOrigin;

    public ProductionEnvironmentValidator(
            Environment environment,
            AuthProperties authProperties,
            @Value("${readstack.frontend-origin}") String frontendOrigin
    ) {
        this.environment = environment;
        this.authProperties = authProperties;
        this.frontendOrigin = frontendOrigin;
    }

    @PostConstruct
    void validate() {
        if ("None".equalsIgnoreCase(authProperties.cookieSameSite()) && !authProperties.cookieSecure()) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE=None の場合は AUTH_COOKIE_SECURE=true が必要です");
        }
        if ("None".equalsIgnoreCase(authProperties.cookieSameSite()) && !authProperties.csrfEnabled()) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE=None の場合は AUTH_CSRF_ENABLED=true が必要です");
        }
        if (isProd(environment) && !authProperties.csrfEnabled()) {
            throw new IllegalStateException("production profile では AUTH_CSRF_ENABLED=true が必要です");
        }
        if (isProd(environment) && (frontendOrigin == null || frontendOrigin.isBlank() || frontendOrigin.contains("localhost"))) {
            throw new IllegalStateException("production profile では FRONTEND_ORIGIN に公開フロントエンドURLを設定してください");
        }
        if (isProd(environment)) {
            validateProductionSecret("JWT_ACCESS_SECRET", authProperties.accessTokenSecret());
            validateProductionSecret("AUTH_REFRESH_TOKEN_HASH_SECRET", authProperties.refreshTokenHashSecret());
        }
    }

    private boolean isProd(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals);
    }

    private void validateProductionSecret(String name, String value) {
        if (value == null || value.isBlank() || value.length() < 32) {
            throw new IllegalStateException("production profile では " + name + " に32文字以上のsecretを設定してください");
        }
        String normalized = value.toLowerCase();
        if (normalized.startsWith("dev-") || normalized.contains("change-me")) {
            throw new IllegalStateException("production profile では " + name + " にdev用secretは使用できません");
        }
    }
}
