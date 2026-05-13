package com.articleshelf.application.auth;

import com.articleshelf.application.observability.BackendMetrics;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthRateLimiter {
    private final AuthRateLimitSettings settings;
    private final AuthRateLimitBucketRepository bucketRepository;
    private final Clock clock;
    private final BackendMetrics metrics;

    @Autowired
    public AuthRateLimiter(
            AuthRateLimitSettings settings,
            AuthRateLimitBucketRepository bucketRepository,
            Clock clock,
            BackendMetrics metrics
    ) {
        this.settings = settings;
        this.bucketRepository = bucketRepository;
        this.clock = clock;
        this.metrics = metrics;
    }

    AuthRateLimiter(AuthRateLimitSettings settings, AuthRateLimitBucketRepository bucketRepository, Clock clock) {
        this(settings, bucketRepository, clock, BackendMetrics.noop());
    }

    @Transactional
    public void checkLogin(String ipAddress, String username) {
        if (!settings.enabled()) {
            return;
        }
        consume(
                "login:" + normalizeIp(ipAddress) + ":" + normalizeUsername(username),
                "login",
                settings.loginCapacity(),
                Duration.ofSeconds(settings.loginWindowSeconds())
        );
    }

    @Transactional
    public void checkRegister(String ipAddress) {
        if (!settings.enabled()) {
            return;
        }
        consume(
                "register:" + normalizeIp(ipAddress),
                "register",
                settings.registerCapacity(),
                Duration.ofSeconds(settings.registerWindowSeconds())
        );
    }

    private void consume(String key, String operation, int capacity, Duration window) {
        if (capacity < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalStateException("auth rate limit capacity and window must be positive");
        }
        Instant now = clock.instant();
        AuthRateLimitBucket bucket = loadOrCreateBucket(key, operation, capacity, now);
        AuthRateLimitBucket consumed = consumeFromBucket(bucket, capacity, window, now);
        if (consumed.tokens() < 0) {
            metrics.recordAuthRateLimited(operation);
            throw new AuthRateLimitExceededException("authentication rate limit exceeded");
        }
        bucketRepository.save(consumed);
        cleanup(now);
    }

    private AuthRateLimitBucket loadOrCreateBucket(String key, String operation, int capacity, Instant now) {
        return bucketRepository.findByKeyForUpdate(key)
                .orElseGet(() -> createOrReloadBucket(key, operation, capacity, now));
    }

    private AuthRateLimitBucket createOrReloadBucket(String key, String operation, int capacity, Instant now) {
        try {
            return bucketRepository.save(new AuthRateLimitBucket(key, operation, capacity, now, now));
        } catch (DataIntegrityViolationException exception) {
            return bucketRepository.findByKeyForUpdate(key)
                    .orElseThrow(() -> exception);
        }
    }

    private AuthRateLimitBucket consumeFromBucket(
            AuthRateLimitBucket current,
            int capacity,
            Duration window,
            Instant now
    ) {
        int tokens = current.tokens();
        Instant windowStartedAt = current.windowStartedAt();
        if (!now.isBefore(windowStartedAt.plus(window))) {
            tokens = capacity;
            windowStartedAt = now;
        }
        if (tokens < 1) {
            return new AuthRateLimitBucket(
                    current.key(),
                    current.operation(),
                    -1,
                    windowStartedAt,
                    now
            );
        }
        return new AuthRateLimitBucket(
                current.key(),
                current.operation(),
                tokens - 1,
                windowStartedAt,
                now
        );
    }

    private void cleanup(Instant now) {
        Duration maxWindow = Duration.ofSeconds(Math.max(settings.loginWindowSeconds(), settings.registerWindowSeconds()));
        bucketRepository.deleteIdleBuckets(now.minus(maxWindow));
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        return ipAddress.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "unknown";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
