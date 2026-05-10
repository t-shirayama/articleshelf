package com.articleshelf.application.auth;

import com.articleshelf.domain.user.UserStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenRotationService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenSecretService refreshTokenSecretService;
    private final AuthSettings settings;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public RefreshTokenRotationService(
            RefreshTokenRepository refreshTokenRepository,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenSecretService refreshTokenSecretService,
            AuthSettings settings,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenSecretService = refreshTokenSecretService;
        this.settings = settings;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public AuthResult issue(AuthUser user, UUID familyId, String userAgent, String ipAddress) {
        CreatedRefreshToken refreshToken = createRefreshToken(user, familyId, userAgent, ipAddress);
        return new AuthResult(toAuthResponse(user), new RefreshSession(refreshToken.rawToken(), issueCsrfToken()));
    }

    public AuthResult rotate(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.invalidRefreshToken("refresh token is missing");
        }
        Instant now = now();
        RefreshTokenRecord current = refreshTokenRepository.findByTokenHash(refreshTokenSecretService.hash(rawRefreshToken))
                .orElseThrow(() -> AuthException.invalidRefreshToken("refresh token is invalid"));
        AuthUser user = current.user();
        if (user.status() != UserStatus.ACTIVE || current.expiresAt().isBefore(now)) {
            throw AuthException.invalidRefreshToken("refresh token is invalid");
        }
        if (current.revokedAt() != null) {
            refreshTokenRepository.revokeFamily(user.id(), current.familyId(), now);
            throw AuthException.invalidRefreshToken("refresh token is invalid");
        }

        CreatedRefreshToken replacement = createRefreshToken(user, current.familyId(), userAgent, ipAddress);
        if (!refreshTokenRepository.replaceIfActive(current.id(), replacement.record().id(), now)) {
            refreshTokenRepository.revoke(replacement.record().id(), now);
            throw AuthException.invalidRefreshToken("refresh token is invalid");
        }
        return new AuthResult(toAuthResponse(user), new RefreshSession(replacement.rawToken(), issueCsrfToken()));
    }

    private CreatedRefreshToken createRefreshToken(AuthUser user, UUID familyId, String userAgent, String ipAddress) {
        String rawToken = refreshTokenSecretService.generateRawToken();
        RefreshTokenRecord token = refreshTokenRepository.create(
                user,
                refreshTokenSecretService.hash(rawToken),
                familyId,
                now().plus(settings.refreshTokenTtlDays(), ChronoUnit.DAYS),
                userAgent,
                ipAddress
        );
        return new CreatedRefreshToken(rawToken, token);
    }

    private AuthResponse toAuthResponse(AuthUser user) {
        CurrentUser currentUser = new CurrentUser(user.id(), user.username(), user.displayName(), List.of(user.role()));
        return new AuthResponse(UserResponse.from(user), accessTokenIssuer.issue(currentUser));
    }

    private String issueCsrfToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private record CreatedRefreshToken(String rawToken, RefreshTokenRecord record) {
    }
}
