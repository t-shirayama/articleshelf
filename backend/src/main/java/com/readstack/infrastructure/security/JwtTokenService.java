package com.readstack.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readstack.application.auth.AccessTokenIssuer;
import com.readstack.application.auth.CurrentUser;
import com.readstack.config.AuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtTokenService implements AccessTokenIssuer {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final AuthProperties properties;
    private final ObjectMapper objectMapper;

    public JwtTokenService(AuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String issue(CurrentUser user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.id().toString());
        payload.put("email", user.email());
        payload.put("roles", user.roles());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(properties.accessTokenTtlSeconds()).getEpochSecond());
        payload.put("jti", UUID.randomUUID().toString());

        String unsigned = encodeJson(header) + "." + encodeJson(payload);
        return unsigned + "." + sign(unsigned);
    }

    public CurrentUser parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("invalid token");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw new JwtValidationException("invalid signature");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        Number exp = (Number) payload.get("exp");
        if (exp == null || Instant.ofEpochSecond(exp.longValue()).isBefore(Instant.now())) {
            throw new JwtValidationException("token expired");
        }

        UUID id = UUID.fromString((String) payload.get("sub"));
        String email = (String) payload.get("email");
        List<String> roles = objectMapper.convertValue(payload.get("roles"), new TypeReference<>() {});
        return new CurrentUser(id, email, email, roles == null ? List.of() : roles);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to encode jwt", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(value), new TypeReference<>() {});
        } catch (Exception exception) {
            throw new JwtValidationException("invalid payload");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.accessTokenSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign jwt", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
