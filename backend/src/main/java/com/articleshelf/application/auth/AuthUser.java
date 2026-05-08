package com.articleshelf.application.auth;

import com.articleshelf.domain.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AuthUser(
        UUID id,
        String username,
        String passwordHash,
        String displayName,
        String role,
        UserStatus status,
        Instant lastLoginAt,
        Instant tokenValidAfter
) {
}
