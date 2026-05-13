package com.articleshelf.application.extension;

import java.time.Instant;
import java.util.UUID;

public record ExtensionAuthCode(
        UUID id,
        String codeHash,
        UUID userId,
        String clientId,
        String extensionId,
        String redirectUri,
        String codeChallenge,
        String codeChallengeMethod,
        Instant expiresAt,
        Instant consumedAt,
        Instant createdAt
) {
    public boolean isUsableAt(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }
}
