package com.articleshelf.application.extension;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.IdGenerator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionAuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-05-12T12:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CLIENT_ID = "articleshelf-chrome-extension-local";
    private static final String EXTENSION_ID = "ncdpeooneagfjhgnhenhakjnfflmpdbj";
    private static final String REDIRECT_URI = "https://ncdpeooneagfjhgnhenhakjnfflmpdbj.chromiumapp.org/";
    private static final String VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    private final InMemoryCodeRepository codes = new InMemoryCodeRepository();
    private final InMemoryTokenRepository tokens = new InMemoryTokenRepository();
    private final MutableClock clock = new MutableClock(NOW);
    private final ExtensionAuthService service = new ExtensionAuthService(
            clientId -> CLIENT_ID.equals(clientId)
                    ? Optional.of(new ExtensionClient(CLIENT_ID, EXTENSION_ID, REDIRECT_URI))
                    : Optional.empty(),
            codes,
            tokens,
            new ExtensionAuthSettings(300, 86400),
            new QueueIdGenerator(),
            clock,
            new SecureRandom(new byte[]{1, 2, 3})
    );
    private final CurrentUser user = new CurrentUser(USER_ID, "reader", "Reader", java.util.List.of("USER"));

    @Test
    void exchangesAuthorizationCodeForScopedOpaqueToken() {
        String code = authorize();

        ExtensionTokenResponse response = service.exchangeToken(
                "authorization_code",
                code,
                REDIRECT_URI,
                CLIENT_ID,
                VERIFIER
        );

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400);
        assertThat(response.scope()).isEqualTo(ExtensionAuthService.DEFAULT_SCOPE);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(tokens.findByTokenHash(service.tokenHash(response.accessToken()))).hasValueSatisfying(token -> {
            assertThat(token.userId()).isEqualTo(USER_ID);
            assertThat(token.clientId()).isEqualTo(CLIENT_ID);
            assertThat(token.extensionId()).isEqualTo(EXTENSION_ID);
            assertThat(token.expiresAt()).isEqualTo(NOW.plusSeconds(86400));
            assertThat(token.scopeSet()).containsExactlyInAnyOrder(
                    ExtensionAuthService.SCOPE_LOOKUP,
                    ExtensionAuthService.SCOPE_CREATE,
                    ExtensionAuthService.SCOPE_UPDATE_STATUS
            );
        });
    }

    @Test
    void rejectsReusedAuthorizationCode() {
        String code = authorize();
        service.exchangeToken("authorization_code", code, REDIRECT_URI, CLIENT_ID, VERIFIER);

        assertThatThrownBy(() -> service.exchangeToken("authorization_code", code, REDIRECT_URI, CLIENT_ID, VERIFIER))
                .isInstanceOf(ExtensionAuthException.class);
    }

    @Test
    void rejectsWrongVerifier() {
        String code = authorize();

        assertThatThrownBy(() -> service.exchangeToken("authorization_code", code, REDIRECT_URI, CLIENT_ID, "wrong-verifier"))
                .isInstanceOf(ExtensionAuthException.class);
    }

    @Test
    void rejectsRedirectUriMismatch() {
        String code = authorize();

        assertThatThrownBy(() -> service.exchangeToken("authorization_code", code, "https://example.com/callback", CLIENT_ID, VERIFIER))
                .isInstanceOf(ExtensionAuthException.class);
    }

    @Test
    void rejectsExpiredAuthorizationCode() {
        String code = authorize();
        clock.instant = NOW.plusSeconds(301);

        assertThatThrownBy(() -> service.exchangeToken("authorization_code", code, REDIRECT_URI, CLIENT_ID, VERIFIER))
                .isInstanceOf(ExtensionAuthException.class);
    }

    @Test
    void authorizeRequiresS256PkceAndKnownClient() {
        assertThatThrownBy(() -> service.authorize(user, CLIENT_ID, EXTENSION_ID, REDIRECT_URI, challenge(), "plain"))
                .isInstanceOf(ExtensionAuthException.class);
        assertThatThrownBy(() -> service.authorize(user, CLIENT_ID, "different-extension", REDIRECT_URI, challenge(), "S256"))
                .isInstanceOf(ExtensionAuthException.class);
    }

    private String authorize() {
        return service.authorize(user, CLIENT_ID, EXTENSION_ID, REDIRECT_URI, challenge(), "S256");
    }

    private static String challenge() {
        return base64Url(sha256(VERIFIER));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static class InMemoryCodeRepository implements ExtensionAuthCodeRepository {
        private final Map<String, ExtensionAuthCode> codes = new LinkedHashMap<>();

        @Override
        public ExtensionAuthCode save(ExtensionAuthCode code) {
            codes.put(code.codeHash(), code);
            return code;
        }

        @Override
        public Optional<ExtensionAuthCode> findByCodeHash(String codeHash) {
            return Optional.ofNullable(codes.get(codeHash));
        }

        @Override
        public void markConsumed(String codeHash, Instant consumedAt) {
            ExtensionAuthCode code = codes.get(codeHash);
            codes.put(codeHash, new ExtensionAuthCode(
                    code.id(),
                    code.codeHash(),
                    code.userId(),
                    code.clientId(),
                    code.extensionId(),
                    code.redirectUri(),
                    code.codeChallenge(),
                    code.codeChallengeMethod(),
                    code.expiresAt(),
                    consumedAt,
                    code.createdAt()
            ));
        }
    }

    private static class InMemoryTokenRepository implements ExtensionAccessTokenRepository {
        private final Map<String, ExtensionAccessToken> tokens = new LinkedHashMap<>();

        @Override
        public ExtensionAccessToken save(ExtensionAccessToken token) {
            tokens.put(token.tokenHash(), token);
            return token;
        }

        @Override
        public Optional<ExtensionAccessToken> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(tokens.get(tokenHash));
        }
    }

    private static class QueueIdGenerator implements IdGenerator {
        private final Queue<UUID> ids = new ArrayDeque<>(java.util.List.of(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                UUID.fromString("00000000-0000-0000-0000-000000000103"),
                UUID.fromString("00000000-0000-0000-0000-000000000104")
        ));

        @Override
        public UUID nextUuid() {
            return ids.remove();
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
