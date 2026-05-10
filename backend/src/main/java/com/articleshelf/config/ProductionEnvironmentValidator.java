package com.articleshelf.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

@Component
public class ProductionEnvironmentValidator {
    private static final Set<String> ALLOWED_POSTGRES_SSL_MODES = Set.of("require", "verify-ca", "verify-full");

    private final Environment environment;
    private final AuthProperties authProperties;
    private final String frontendOrigin;
    private final String datasourceUrl;

    public ProductionEnvironmentValidator(
            Environment environment,
            AuthProperties authProperties,
            @Value("${articleshelf.frontend-origin}") String frontendOrigin,
            @Value("${spring.datasource.url:}") String datasourceUrl
    ) {
        this.environment = environment;
        this.authProperties = authProperties;
        this.frontendOrigin = frontendOrigin;
        this.datasourceUrl = datasourceUrl;
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
            validateProductionDatasourceTls(datasourceUrl);
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

    private void validateProductionDatasourceTls(String datasourceUrl) {
        String sslMode = extractJdbcQueryParameter(datasourceUrl, "sslmode");
        if (sslMode == null || !ALLOWED_POSTGRES_SSL_MODES.contains(sslMode.toLowerCase())) {
            throw new IllegalStateException(
                    "production profile では SPRING_DATASOURCE_URL に sslmode=require 以上を設定してください"
            );
        }
    }

    private String extractJdbcQueryParameter(String jdbcUrl, String parameterName) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }

        int queryStart = jdbcUrl.indexOf('?');
        if (queryStart < 0 || queryStart == jdbcUrl.length() - 1) {
            return null;
        }

        String[] parameters = jdbcUrl.substring(queryStart + 1).split("&");
        for (String parameter : parameters) {
            String[] keyValue = parameter.split("=", 2);
            if (keyValue.length == 2 && parameterName.equalsIgnoreCase(decode(keyValue[0]))) {
                return decode(keyValue[1]);
            }
        }

        return null;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
