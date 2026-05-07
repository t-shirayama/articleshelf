package com.readstack.application.auth;

public record AuthResponse(UserResponse user, String accessToken) {
}
