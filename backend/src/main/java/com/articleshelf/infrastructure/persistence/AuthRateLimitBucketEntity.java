package com.articleshelf.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_rate_limit_buckets")
public class AuthRateLimitBucketEntity {
    @Id
    @Column(name = "bucket_key", nullable = false, length = 256)
    private String key;

    @Column(nullable = false, length = 32)
    private String operation;

    @Column(nullable = false)
    private int tokens;

    @Column(nullable = false)
    private Instant windowStartedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public Instant getWindowStartedAt() {
        return windowStartedAt;
    }

    public void setWindowStartedAt(Instant windowStartedAt) {
        this.windowStartedAt = windowStartedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
