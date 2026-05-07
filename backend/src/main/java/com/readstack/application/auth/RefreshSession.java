package com.readstack.application.auth;

public record RefreshSession(String rawRefreshToken, String csrfToken) {
}
