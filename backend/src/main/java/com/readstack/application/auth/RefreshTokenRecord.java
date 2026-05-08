package com.readstack.application.auth;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenRecord(
        UUID id,
        AuthUser user,
        UUID familyId,
        Instant expiresAt,
        Instant revokedAt
) {
}
