package com.readstack.infrastructure.security;

import com.readstack.application.auth.AccessTokenIssuer;
import com.readstack.application.auth.CurrentUser;
import com.readstack.config.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService implements AccessTokenIssuer {
    private final AuthProperties properties;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtTokenService(AuthProperties properties) {
        this.properties = properties;
        SecretKey secretKey = new SecretKeySpec(
                properties.accessTokenSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Override
    public String issue(CurrentUser user) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("username", user.username())
                .claim("roles", user.roles())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.accessTokenTtlSeconds()))
                .id(UUID.randomUUID().toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public CurrentUser parse(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            UUID id = UUID.fromString(jwt.getSubject());
            String username = jwt.getClaimAsString("username");
            List<String> roles = jwt.getClaimAsStringList("roles");
            return new CurrentUser(id, username, username, roles == null ? List.of() : roles, jwt.getIssuedAt());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtValidationException("invalid token");
        }
    }
}
