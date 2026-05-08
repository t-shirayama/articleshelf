package com.articleshelf.application.auth;

public record AuthResult(AuthResponse response, RefreshSession session) {
}
