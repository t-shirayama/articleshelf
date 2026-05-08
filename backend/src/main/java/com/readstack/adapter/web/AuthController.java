package com.readstack.adapter.web;

import com.readstack.application.auth.AuthResponse;
import com.readstack.application.auth.AuthResult;
import com.readstack.application.auth.AuthException;
import com.readstack.application.auth.AuthSessionSettings;
import com.readstack.application.auth.AuthService;
import com.readstack.application.auth.CurrentUser;
import com.readstack.application.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "READSTACK_REFRESH";
    private static final String CSRF_COOKIE = "READSTACK_CSRF";

    private final AuthService authService;
    private final AuthSessionSettings settings;

    public AuthController(AuthService authService, AuthSessionSettings settings) {
        this.authService = authService;
        this.settings = settings;
    }

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        AuthResult result = authService.register(
                request.username(),
                request.password(),
                request.displayName(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        setSessionCookies(response, result);
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(
                request.username(),
                request.password(),
                servletRequest.getHeader("User-Agent"),
                servletRequest.getRemoteAddr()
        );
        setSessionCookies(response, result);
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        validateCsrf(csrfCookie, csrfHeader);
        try {
            AuthResult result = authService.refresh(refreshToken, servletRequest.getHeader("User-Agent"), servletRequest.getRemoteAddr());
            setSessionCookies(response, result);
            return result.response();
        } catch (AuthException exception) {
            clearSessionCookies(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(NO_CONTENT)
    public void logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletResponse response
    ) {
        validateCsrf(csrfCookie, csrfHeader);
        authService.logout(refreshToken);
        clearSessionCookies(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal CurrentUser currentUser, HttpServletResponse response) {
        authService.logoutAll(currentUser);
        clearSessionCookies(response);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.currentUser(currentUser);
    }

    private void validateCsrf(String csrfCookie, String csrfHeader) {
        if (!settings.csrfEnabled()) {
            return;
        }
        if (csrfCookie == null || csrfCookie.isBlank() || !csrfCookie.equals(csrfHeader)) {
            throw new CsrfValidationException();
        }
    }

    private void setSessionCookies(HttpServletResponse response, AuthResult result) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, result.session().rawRefreshToken(), true, refreshMaxAge()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE, result.session().csrfToken(), false, refreshMaxAge()).toString());
    }

    private void clearSessionCookies(HttpServletResponse response) {
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

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            String displayName
    ) {
    }

    public record LoginRequest(@NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username, @NotBlank String password) {
    }

    public static class CsrfValidationException extends RuntimeException {
        public CsrfValidationException() {
            super("CSRF token is invalid");
        }
    }
}
