package com.articleshelf.application.auth;

public interface RefreshTokenSecretService {
    String generateRawToken();

    String hash(String rawToken);
}
