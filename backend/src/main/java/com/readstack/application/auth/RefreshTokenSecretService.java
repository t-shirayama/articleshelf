package com.readstack.application.auth;

public interface RefreshTokenSecretService {
    String generateRawToken();

    String hash(String rawToken);
}
