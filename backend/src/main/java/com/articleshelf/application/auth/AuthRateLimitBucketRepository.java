package com.articleshelf.application.auth;

import java.time.Instant;
import java.util.Optional;

public interface AuthRateLimitBucketRepository {
    Optional<AuthRateLimitBucket> findByKeyForUpdate(String key);

    AuthRateLimitBucket save(AuthRateLimitBucket bucket);

    void deleteIdleBuckets(Instant olderThan);
}
