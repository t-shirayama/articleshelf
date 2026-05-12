package com.articleshelf.infrastructure.persistence;

import com.articleshelf.application.auth.AuthRateLimitBucket;
import com.articleshelf.application.auth.AuthRateLimitBucketRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaAuthRateLimitBucketRepository implements AuthRateLimitBucketRepository {
    private final SpringDataAuthRateLimitBucketJpaRepository bucketJpaRepository;

    public JpaAuthRateLimitBucketRepository(SpringDataAuthRateLimitBucketJpaRepository bucketJpaRepository) {
        this.bucketJpaRepository = bucketJpaRepository;
    }

    @Override
    public Optional<AuthRateLimitBucket> findByKeyForUpdate(String key) {
        return bucketJpaRepository.findByKeyForUpdate(key).map(this::toApplication);
    }

    @Override
    public AuthRateLimitBucket save(AuthRateLimitBucket bucket) {
        AuthRateLimitBucketEntity entity = bucketJpaRepository.findById(bucket.key())
                .orElseGet(AuthRateLimitBucketEntity::new);
        entity.setKey(bucket.key());
        entity.setOperation(bucket.operation());
        entity.setTokens(bucket.tokens());
        entity.setWindowStartedAt(bucket.windowStartedAt());
        entity.setUpdatedAt(bucket.updatedAt());
        return toApplication(bucketJpaRepository.save(entity));
    }

    @Override
    public void deleteIdleBuckets(Instant olderThan) {
        bucketJpaRepository.deleteIdleBuckets(olderThan);
    }

    private AuthRateLimitBucket toApplication(AuthRateLimitBucketEntity entity) {
        return new AuthRateLimitBucket(
                entity.getKey(),
                entity.getOperation(),
                entity.getTokens(),
                entity.getWindowStartedAt(),
                entity.getUpdatedAt()
        );
    }
}
