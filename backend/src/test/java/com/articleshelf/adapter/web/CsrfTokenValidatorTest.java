package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthSessionSettings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsrfTokenValidatorTest {
    @Test
    void acceptsMatchingCsrfCookieAndHeaderWhenEnabled() {
        CsrfTokenValidator validator = new CsrfTokenValidator(settings(true));

        assertThatCode(() -> validator.validate("token", "token")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrMismatchedCsrfTokenWhenEnabled() {
        CsrfTokenValidator validator = new CsrfTokenValidator(settings(true));

        assertThatThrownBy(() -> validator.validate("token", "other"))
                .isInstanceOf(CsrfValidationException.class);
        assertThatThrownBy(() -> validator.validate(null, "token"))
                .isInstanceOf(CsrfValidationException.class);
    }

    @Test
    void skipsValidationWhenDisabled() {
        CsrfTokenValidator validator = new CsrfTokenValidator(settings(false));

        assertThatCode(() -> validator.validate(null, null)).doesNotThrowAnyException();
    }

    private AuthSessionSettings settings(boolean csrfEnabled) {
        return new TestAuthSessionSettings(csrfEnabled);
    }

    private record TestAuthSessionSettings(boolean csrfEnabled) implements AuthSessionSettings {
        @Override
        public long refreshTokenTtlDays() {
            return 30;
        }

        @Override
        public boolean cookieSecure() {
            return false;
        }

        @Override
        public String cookieSameSite() {
            return "Lax";
        }
    }
}
