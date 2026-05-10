package com.articleshelf.application.auth;

import com.articleshelf.application.observability.BackendMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimiter {
    private final AuthRateLimitSettings settings;
    private final Clock clock;
    private final BackendMetrics metrics;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public AuthRateLimiter(AuthRateLimitSettings settings, BackendMetrics metrics) {
        this(settings, Clock.systemUTC(), metrics);
    }

    AuthRateLimiter(AuthRateLimitSettings settings, Clock clock) {
        this(settings, clock, BackendMetrics.noop());
    }

    AuthRateLimiter(AuthRateLimitSettings settings, Clock clock, BackendMetrics metrics) {
        this.settings = settings;
        this.clock = clock;
        this.metrics = metrics;
    }

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
        long now = clock.millis();
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, now));
        if (!bucket.tryConsume(capacity, window.toMillis(), now)) {
            metrics.recordAuthRateLimited(operation);
            throw new AuthRateLimitExceededException("authentication rate limit exceeded");
        }
        cleanup(now);
    }

    private void cleanup(long now) {
        long maxIdleMillis = Duration.ofSeconds(Math.max(settings.loginWindowSeconds(), settings.registerWindowSeconds())).toMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isIdle(now, maxIdleMillis));
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

    private static class Bucket {
        private int tokens;
        private long windowStartedAt;
        private long lastTouchedAt;

        Bucket(int capacity, long now) {
            this.tokens = capacity;
            this.windowStartedAt = now;
            this.lastTouchedAt = now;
        }

        synchronized boolean tryConsume(int capacity, long windowMillis, long now) {
            if (now - windowStartedAt >= windowMillis) {
                tokens = capacity;
                windowStartedAt = now;
            }
            lastTouchedAt = now;
            if (tokens < 1) {
                return false;
            }
            tokens--;
            return true;
        }

        synchronized boolean isIdle(long now, long maxIdleMillis) {
            return now - lastTouchedAt > maxIdleMillis;
        }
    }
}
