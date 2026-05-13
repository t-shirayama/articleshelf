package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataAuthRateLimitBucketJpaRepository extends JpaRepository<AuthRateLimitBucketEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bucket from AuthRateLimitBucketEntity bucket where bucket.key = :key")
    Optional<AuthRateLimitBucketEntity> findByKeyForUpdate(@Param("key") String key);

    @Modifying
    @Query("delete from AuthRateLimitBucketEntity bucket where bucket.updatedAt < :olderThan")
    void deleteIdleBuckets(@Param("olderThan") Instant olderThan);
}
