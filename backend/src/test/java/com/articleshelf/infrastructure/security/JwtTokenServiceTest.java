package com.articleshelf.infrastructure.security;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.IdGenerator;
import com.articleshelf.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    private static final String ACCESS_TOKEN_SECRET = "test-articleshelf-access-secret-change-me-please-32bytes";
    private static final Instant FIXED_NOW = Instant.parse("2026-05-01T03:04:05Z");
    private static final UUID FIXED_JTI = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void issuesAndParsesAccessToken() {
        JwtTokenService service = jwtTokenService(900);
        CurrentUser user = new CurrentUser(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), "reader", "User", List.of("USER"));

        CurrentUser parsed = service.parse(service.issue(user));

        assertThat(parsed.id()).isEqualTo(user.id());
        assertThat(parsed.username()).isEqualTo(user.username());
        assertThat(parsed.roles()).containsExactly("USER");
        assertThat(parsed.tokenIssuedAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void issuesTokenWithInjectedIssuedAtExpiryAndJti() {
        JwtTokenService service = jwtTokenService(900);
        CurrentUser user = new CurrentUser(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), "reader", "User", List.of("USER"));

        Jwt jwt = decode(service.issue(user));

        assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_NOW.plusSeconds(900));
        assertThat(jwt.getId()).isEqualTo(FIXED_JTI.toString());
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService service = jwtTokenService(900);
        String token = service.issue(new CurrentUser(UUID.randomUUID(), "reader", "User", List.of("USER")));

        assertThatThrownBy(() -> service.parse(token + "x"))
                .isInstanceOf(JwtValidationException.class)
                .hasMessage("invalid token");
    }

    @Test
    void rejectsUnexpectedAlgorithmHeader() {
        JwtTokenService service = jwtTokenService(900);
        String token = service.issue(new CurrentUser(UUID.randomUUID(), "reader", "User", List.of("USER")));
        String[] parts = token.split("\\.");
        String noneHeader = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String altered = noneHeader + "." + parts[1] + "." + parts[2];

        assertThatThrownBy(() -> service.parse(altered))
                .isInstanceOf(JwtValidationException.class)
                .hasMessage("invalid token");
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService service = jwtTokenService(900);
        String token = issueExpiredToken();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessage("invalid token");
    }

    private AuthProperties authProperties(long accessTokenTtlSeconds) {
        return new AuthProperties(
                ACCESS_TOKEN_SECRET,
                accessTokenTtlSeconds,
                "test-articleshelf-refresh-hash-secret-change-me",
                30,
                false,
                "Lax",
                false,
                false,
                "owner",
                "password123"
        );
    }

    private JwtTokenService jwtTokenService(long accessTokenTtlSeconds) {
        return new JwtTokenService(
                authProperties(accessTokenTtlSeconds),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                fixedIdGenerator()
        );
    }

    private IdGenerator fixedIdGenerator() {
        return () -> FIXED_JTI;
    }

    private Jwt decode(String token) {
        return jwtDecoder().decode(token);
    }

    private JwtDecoder jwtDecoder() {
        var secretKey = new SecretKeySpec(ACCESS_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        var timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        decoder.setJwtValidator(timestampValidator);
        return decoder;
    }

    private String issueExpiredToken() {
        var secretKey = new SecretKeySpec(ACCESS_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", "reader")
                .claim("roles", List.of("USER"))
                .issuedAt(FIXED_NOW.minusSeconds(120))
                .expiresAt(FIXED_NOW.minusSeconds(61))
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
