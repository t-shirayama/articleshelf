package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthService;
import com.articleshelf.application.auth.AuthSessionSettings;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final String REFRESH_COOKIE = "ARTICLESHELF_REFRESH";
    private static final String CSRF_COOKIE = "ARTICLESHELF_CSRF";

    private final AuthService authService;
    private final AuthSessionSettings settings;

    public UserController(AuthService authService, AuthSessionSettings settings) {
        this.authService = authService;
        this.settings = settings;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.currentUser(currentUser);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        authService.changePassword(currentUser, request.currentPassword(), request.newPassword());
        clearSessionCookies(response);
    }

    @DeleteMapping("/me")
    @ResponseStatus(NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletResponse response
    ) {
        authService.deleteAccount(currentUser, request.currentPassword());
        clearSessionCookies(response);
    }

    private void clearSessionCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(CSRF_COOKIE).toString());
    }

    private ResponseCookie cookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(REFRESH_COOKIE.equals(name))
                .secure(settings.cookieSecure())
                .sameSite(settings.cookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record DeleteAccountRequest(@NotBlank String currentPassword) {
    }
}
