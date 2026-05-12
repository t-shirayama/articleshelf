package com.articleshelf.application.extension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ExtensionAccessToken(
        UUID id,
        String tokenHash,
        UUID userId,
        String clientId,
        String extensionId,
        String scopes,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public Set<String> scopeSet() {
        return Arrays.stream(scopes.split(" "))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
