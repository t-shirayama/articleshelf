package com.articleshelf.application.auth;

public record RefreshSession(String rawRefreshToken, String csrfToken) {
}
