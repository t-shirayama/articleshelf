package com.readstack.config;

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
                "https://readstack.example.com"
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
                "https://readstack.example.com"
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private AuthProperties authProperties(boolean cookieSecure, String cookieSameSite, boolean csrfEnabled) {
        return new AuthProperties(
                "test-readstack-access-secret-change-me-please-32bytes",
                900,
                "test-readstack-refresh-hash-secret-change-me",
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
