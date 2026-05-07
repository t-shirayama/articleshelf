package com.readstack.infrastructure.security;

import com.readstack.application.auth.RefreshTokenSecretService;
import com.readstack.config.AuthProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RefreshTokenHashService implements RefreshTokenSecretService {
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenHashService(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.refreshTokenHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hashed = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash refresh token", exception);
        }
    }
}
