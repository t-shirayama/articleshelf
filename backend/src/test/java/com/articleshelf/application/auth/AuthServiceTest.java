package com.articleshelf.application.auth;

import com.articleshelf.domain.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-05-10T12:34:56Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FAMILY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID REFRESH_TOKEN_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void registerUsesInjectedClockAndIdGeneratorForTokenFamily() {
        AuthUserRepository users = mock(AuthUserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
        RefreshTokenSecretService refreshTokenSecretService = mock(RefreshTokenSecretService.class);
        AuthSettings settings = mock(AuthSettings.class);

        when(users.existsByUsername("reader")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("hashed-password");
        when(accessTokenIssuer.issue(any(CurrentUser.class))).thenReturn("access-token");
        when(refreshTokenSecretService.generateRawToken()).thenReturn("raw-refresh-token");
        when(refreshTokenSecretService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(settings.refreshTokenTtlDays()).thenReturn(14L);
        when(users.save(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser user = invocation.getArgument(0);
            return new AuthUser(
                    USER_ID,
                    user.username(),
                    user.passwordHash(),
                    user.displayName(),
                    user.role(),
                    user.status(),
                    user.lastLoginAt(),
                    user.tokenValidAfter()
            );
        });
        when(refreshTokens.create(any(), eq("hashed-refresh-token"), eq(FAMILY_ID), any(), eq("JUnit"), eq("127.0.0.1")))
                .thenAnswer(invocation -> new RefreshTokenRecord(
                        REFRESH_TOKEN_ID,
                        invocation.getArgument(0),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        null
                ));

        AuthService service = new AuthService(
                users,
                refreshTokens,
                passwordHasher,
                refreshTokenSecretService,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                () -> FAMILY_ID,
                new RefreshTokenRotationService(
                        refreshTokens,
                        accessTokenIssuer,
                        refreshTokenSecretService,
                        settings,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        new SecureRandom(new byte[]{1, 2, 3})
                ),
                new InitialUserProvisioner(users, passwordHasher, settings)
        );

        AuthResult result = service.register("Reader", "password123", "", "JUnit", "127.0.0.1");

        assertThat(result.response().user().id()).isEqualTo(USER_ID);
        assertThat(result.session().rawRefreshToken()).isEqualTo("raw-refresh-token");
        verify(users).save(new AuthUser(
                null,
                "reader",
                "hashed-password",
                "reader",
                "USER",
                UserStatus.ACTIVE,
                FIXED_NOW,
                Instant.EPOCH
        ));
        verify(refreshTokens).create(
                any(AuthUser.class),
                eq("hashed-refresh-token"),
                eq(FAMILY_ID),
                eq(FIXED_NOW.plus(14, ChronoUnit.DAYS)),
                eq("JUnit"),
                eq("127.0.0.1")
        );
    }

    @Test
    void logoutAllUsesInjectedClockForTokenInvalidation() {
        AuthUserRepository users = mock(AuthUserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        AuthUser user = new AuthUser(
                USER_ID,
                "reader",
                "hashed-password",
                "Reader",
                "USER",
                UserStatus.ACTIVE,
                FIXED_NOW.minus(1, ChronoUnit.DAYS),
                Instant.EPOCH
        );
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(users.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthService service = new AuthService(
                users,
                refreshTokens,
                mock(PasswordHasher.class),
                mock(RefreshTokenSecretService.class),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                () -> FAMILY_ID,
                mock(RefreshTokenRotationService.class),
                mock(InitialUserProvisioner.class)
        );

        service.logoutAll(new CurrentUser(USER_ID, "reader", "Reader", java.util.List.of("USER")));

        verify(users).save(new AuthUser(
                USER_ID,
                "reader",
                "hashed-password",
                "Reader",
                "USER",
                UserStatus.ACTIVE,
                FIXED_NOW.minus(1, ChronoUnit.DAYS),
                FIXED_NOW
        ));
        verify(refreshTokens).revokeAllByUserId(USER_ID, FIXED_NOW);
    }

    @Test
    void refreshRevokesTokenFamilyWhenAtomicReplaceDoesNotUpdateCurrentToken() {
        AuthUserRepository users = mock(AuthUserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
        RefreshTokenSecretService refreshTokenSecretService = mock(RefreshTokenSecretService.class);
        AuthSettings settings = mock(AuthSettings.class);
        AuthUser user = new AuthUser(
                USER_ID,
                "reader",
                "hashed-password",
                "Reader",
                "USER",
                UserStatus.ACTIVE,
                FIXED_NOW.minus(1, ChronoUnit.DAYS),
                Instant.EPOCH
        );
        RefreshTokenRecord current = new RefreshTokenRecord(
                REFRESH_TOKEN_ID,
                user,
                FAMILY_ID,
                FIXED_NOW.plus(1, ChronoUnit.DAYS),
                null
        );
        UUID replacementId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        RefreshTokenRecord replacement = new RefreshTokenRecord(
                replacementId,
                user,
                FAMILY_ID,
                FIXED_NOW.plus(14, ChronoUnit.DAYS),
                null
        );
        when(refreshTokenSecretService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenSecretService.generateRawToken()).thenReturn("raw-replacement-token");
        when(refreshTokens.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(current));
        when(refreshTokens.create(any(), eq("hashed-replacement-token"), eq(FAMILY_ID), any(), eq("JUnit"), eq("127.0.0.1")))
                .thenReturn(replacement);
        when(refreshTokenSecretService.hash("raw-replacement-token")).thenReturn("hashed-replacement-token");
        when(settings.refreshTokenTtlDays()).thenReturn(14L);
        when(refreshTokens.replaceIfActive(REFRESH_TOKEN_ID, replacementId, FIXED_NOW)).thenReturn(false);

        AuthService service = new AuthService(
                users,
                refreshTokens,
                mock(PasswordHasher.class),
                refreshTokenSecretService,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                () -> FAMILY_ID,
                new RefreshTokenRotationService(
                        refreshTokens,
                        accessTokenIssuer,
                        refreshTokenSecretService,
                        settings,
                        Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                        new SecureRandom(new byte[]{1, 2, 3})
                ),
                mock(InitialUserProvisioner.class)
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.refresh("raw-refresh-token", "JUnit", "127.0.0.1"))
                .isInstanceOf(AuthException.class);

        verify(refreshTokens).revokeFamily(USER_ID, FAMILY_ID, FIXED_NOW);
        verifyNoInteractions(accessTokenIssuer);
    }
}
