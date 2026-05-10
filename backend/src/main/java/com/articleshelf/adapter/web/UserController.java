package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthService;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;
    private final SessionCookieWriter sessionCookieWriter;

    public UserController(AuthService authService, SessionCookieWriter sessionCookieWriter) {
        this.authService = authService;
        this.sessionCookieWriter = sessionCookieWriter;
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
        sessionCookieWriter.clearSession(response);
    }

    @DeleteMapping("/me")
    @ResponseStatus(NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody DeleteAccountRequest request,
        HttpServletResponse response
    ) {
        authService.deleteAccount(currentUser, request.currentPassword());
        sessionCookieWriter.clearSession(response);
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record DeleteAccountRequest(@NotBlank String currentPassword) {
    }
}
