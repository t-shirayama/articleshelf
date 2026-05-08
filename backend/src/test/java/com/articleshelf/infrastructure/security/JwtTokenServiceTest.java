package com.articleshelf.infrastructure.security;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {
    private static final String ACCESS_TOKEN_SECRET = "test-articleshelf-access-secret-change-me-please-32bytes";

    @Test
    void issuesAndParsesAccessToken() {
        JwtTokenService service = new JwtTokenService(authProperties(900));
        CurrentUser user = new CurrentUser(UUID.randomUUID(), "reader", "User", List.of("USER"));

        CurrentUser parsed = service.parse(service.issue(user));

        assertThat(parsed.id()).isEqualTo(user.id());
        assertThat(parsed.username()).isEqualTo(user.username());
        assertThat(parsed.roles()).containsExactly("USER");
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService service = new JwtTokenService(authProperties(900));
        String token = service.issue(new CurrentUser(UUID.randomUUID(), "reader", "User", List.of("USER")));

        assertThatThrownBy(() -> service.parse(token + "x"))
                .isInstanceOf(JwtValidationException.class)
                .hasMessage("invalid token");
    }

    @Test
    void rejectsUnexpectedAlgorithmHeader() {
        JwtTokenService service = new JwtTokenService(authProperties(900));
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
        JwtTokenService service = new JwtTokenService(authProperties(900));
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

    private String issueExpiredToken() {
        var secretKey = new SecretKeySpec(ACCESS_TOKEN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        Instant now = Instant.now();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", "reader")
                .claim("roles", List.of("USER"))
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(61))
                .id(UUID.randomUUID().toString())
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
