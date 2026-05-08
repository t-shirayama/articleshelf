package com.articleshelf.application.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    RefreshTokenRecord create(AuthUser user, String tokenHash, UUID familyId, Instant expiresAt, String userAgent, String ipAddress);

    void replace(UUID currentId, UUID replacementId, Instant revokedAt);

    void revoke(UUID id, Instant revokedAt);

    void revokeFamily(UUID userId, UUID familyId, Instant revokedAt);

    void revokeAllByUserId(UUID userId, Instant revokedAt);
}
