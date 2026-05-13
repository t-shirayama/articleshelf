package com.articleshelf.application.auth;

import com.articleshelf.config.AuthRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {
    @Test
    void loginLimitUsesIpAndNormalizedUsername() {
        MutableClock clock = new MutableClock();
        InMemoryBucketRepository bucketRepository = new InMemoryBucketRepository();
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(true, 2, 60, 3, 600),
                bucketRepository,
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
        InMemoryBucketRepository bucketRepository = new InMemoryBucketRepository();
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(true, 5, 60, 1, 600),
                bucketRepository,
                clock
        );

        limiter.checkRegister("203.0.113.10");
        assertThatThrownBy(() -> limiter.checkRegister("203.0.113.10"))
                .isInstanceOf(AuthRateLimitExceededException.class);

        clock.advanceMillis(600_000);

        assertThatCode(() -> limiter.checkRegister("203.0.113.10")).doesNotThrowAnyException();
    }

    @Test
    void sharedRepositoryAppliesLimitsAcrossLimiterInstances() {
        MutableClock clock = new MutableClock();
        InMemoryBucketRepository bucketRepository = new InMemoryBucketRepository();
        AuthRateLimitProperties settings = new AuthRateLimitProperties(true, 1, 60, 1, 600);
        AuthRateLimiter firstLimiter = new AuthRateLimiter(settings, bucketRepository, clock);
        AuthRateLimiter secondLimiter = new AuthRateLimiter(settings, bucketRepository, clock);

        firstLimiter.checkLogin("203.0.113.40", "reader");

        assertThatThrownBy(() -> secondLimiter.checkLogin("203.0.113.40", "reader"))
                .isInstanceOf(AuthRateLimitExceededException.class);
    }

    @Test
    void disabledLimiterAllowsRequests() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                new AuthRateLimitProperties(false, 1, 60, 1, 600),
                new InMemoryBucketRepository(),
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

    private static class InMemoryBucketRepository implements AuthRateLimitBucketRepository {
        private final Map<String, AuthRateLimitBucket> buckets = new HashMap<>();

        @Override
        public Optional<AuthRateLimitBucket> findByKeyForUpdate(String key) {
            return Optional.ofNullable(buckets.get(key));
        }

        @Override
        public AuthRateLimitBucket save(AuthRateLimitBucket bucket) {
            buckets.put(bucket.key(), bucket);
            return bucket;
        }

        @Override
        public void deleteIdleBuckets(Instant olderThan) {
            buckets.entrySet().removeIf(entry -> entry.getValue().updatedAt().isBefore(olderThan));
        }
    }
}
