package com.readstack.application.auth;

import com.readstack.config.AuthRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {
    @Test
    void loginLimitUsesIpAndNormalizedUsername() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(true, 2, 60, 3, 600),
                clock
        );

        limiter.checkLogin("203.0.113.10", "Reader");
        limiter.checkLogin("203.0.113.10", " reader ");

        assertThatThrownBy(() -> limiter.checkLogin("203.0.113.10", "READER"))
                .isInstanceOf(AuthRateLimitExceededException.class);
        assertThatCode(() -> limiter.checkLogin("203.0.113.10", "other-reader")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkLogin("203.0.113.11", "reader")).doesNotThrowAnyException();
    }

    @Test
    void registerLimitUsesIpAndRefillsAfterWindow() {
        MutableClock clock = new MutableClock();
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(true, 5, 60, 1, 600),
                clock
        );

        limiter.checkRegister("203.0.113.10");
        assertThatThrownBy(() -> limiter.checkRegister("203.0.113.10"))
                .isInstanceOf(AuthRateLimitExceededException.class);

        clock.advanceMillis(600_000);

        assertThatCode(() -> limiter.checkRegister("203.0.113.10")).doesNotThrowAnyException();
    }

    @Test
    void disabledLimiterAllowsRequests() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(false, 1, 60, 1, 600),
                new MutableClock()
        );

        assertThatCode(() -> {
            limiter.checkLogin("203.0.113.10", "reader");
            limiter.checkLogin("203.0.113.10", "reader");
            limiter.checkRegister("203.0.113.10");
            limiter.checkRegister("203.0.113.10");
        }).doesNotThrowAnyException();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-05-08T00:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
