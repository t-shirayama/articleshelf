package com.readstack.application.auth;

public record AuthResult(AuthResponse response, RefreshSession session) {
}
