package com.articleshelf.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionEnvironmentValidatorTest {
    @Test
    void rejectsDisabledCsrfInProductionProfile() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                authProperties(true, "Lax", false),
                "https://articleshelf.example.com"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("production profile では AUTH_CSRF_ENABLED=true が必要です");
    }

    @Test
    void rejectsSameSiteNoneWithoutSecureCookie() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                new MockEnvironment(),
                authProperties(false, "None", true),
                "http://localhost:5173"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AUTH_COOKIE_SAME_SITE=None の場合は AUTH_COOKIE_SECURE=true が必要です");
    }

    @Test
    void acceptsSecureCrossSiteProductionSettings() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                authProperties(true, "None", true),
                "https://articleshelf.example.com"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsDevSecretsInProductionProfile() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                authProperties(
                        true,
                        "None",
                        true,
                        "dev-articleshelf-access-secret-change-me-please-32bytes",
                        "prod-refresh-token-secret-with-enough-length"
                ),
                "https://articleshelf.example.com"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("production profile では JWT_ACCESS_SECRET にdev用secretは使用できません");
    }

    @Test
    void rejectsShortSecretsInProductionProfile() {
        ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                authProperties(
                        true,
                        "None",
                        true,
                        "short",
                        "prod-refresh-token-secret-with-enough-length"
                ),
                "https://articleshelf.example.com"
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("production profile では JWT_ACCESS_SECRET に32文字以上のsecretを設定してください");
    }

    private AuthProperties authProperties(boolean cookieSecure, String cookieSameSite, boolean csrfEnabled) {
        return authProperties(
                cookieSecure,
                cookieSameSite,
                csrfEnabled,
                "prod-access-token-secret-with-enough-length",
                "prod-refresh-token-secret-with-enough-length"
        );
    }

    private AuthProperties authProperties(
            boolean cookieSecure,
            String cookieSameSite,
            boolean csrfEnabled,
            String accessTokenSecret,
            String refreshTokenHashSecret
    ) {
        return new AuthProperties(
                accessTokenSecret,
                900,
                refreshTokenHashSecret,
                30,
                cookieSecure,
                cookieSameSite,
                csrfEnabled,
                false,
                "owner",
                "password123"
        );
    }
}
