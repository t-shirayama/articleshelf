package com.articleshelf.application.auth;

public record AuthResponse(UserResponse user, String accessToken) {
}
