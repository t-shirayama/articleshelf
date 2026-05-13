package com.articleshelf.application.auth;

import java.time.Instant;

public record AuthRateLimitBucket(
        String key,
        String operation,
        int tokens,
        Instant windowStartedAt,
        Instant updatedAt
) {
}
