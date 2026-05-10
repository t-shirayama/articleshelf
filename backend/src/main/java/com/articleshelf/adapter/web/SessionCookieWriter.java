package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthResult;
import com.articleshelf.application.auth.AuthSessionSettings;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class SessionCookieWriter {
    public static final String REFRESH_COOKIE = "ARTICLESHELF_REFRESH";
    public static final String CSRF_COOKIE = "ARTICLESHELF_CSRF";

    private final AuthSessionSettings settings;

    public SessionCookieWriter(AuthSessionSettings settings) {
        this.settings = settings;
    }

    public void writeSession(HttpServletResponse response, AuthResult result) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, result.session().rawRefreshToken(), true, refreshMaxAge()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, result.session().csrfToken(), false, refreshMaxAge()).toString());
    }

    public void clearSession(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", true, Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, "", false, Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(settings.cookieSecure())
                .sameSite(settings.cookieSameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private Duration refreshMaxAge() {
        return Duration.ofDays(settings.refreshTokenTtlDays());
    }
}
