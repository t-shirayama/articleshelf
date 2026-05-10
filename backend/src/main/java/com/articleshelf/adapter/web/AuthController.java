package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthResponse;
import com.articleshelf.application.auth.AuthResult;
import com.articleshelf.application.auth.AuthException;
import com.articleshelf.application.auth.AuthService;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthAttemptGuard authAttemptGuard;
    private final SessionCookieWriter sessionCookieWriter;
    private final CsrfTokenValidator csrfTokenValidator;
    private final ClientRequestContext clientRequestContext;

    public AuthController(
            AuthService authService,
            AuthAttemptGuard authAttemptGuard,
            SessionCookieWriter sessionCookieWriter,
            CsrfTokenValidator csrfTokenValidator,
            ClientRequestContext clientRequestContext
    ) {
        this.authService = authService;
        this.authAttemptGuard = authAttemptGuard;
        this.sessionCookieWriter = sessionCookieWriter;
        this.csrfTokenValidator = csrfTokenValidator;
        this.clientRequestContext = clientRequestContext;
    }

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        authAttemptGuard.checkRegister(servletRequest);
        AuthResult result = authService.register(
                request.username(),
                request.password(),
                request.displayName(),
                clientRequestContext.userAgent(servletRequest),
                clientRequestContext.ipAddress(servletRequest)
        );
        sessionCookieWriter.writeSession(response, result);
        return result.response();
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        authAttemptGuard.checkLogin(servletRequest, request.username());
        AuthResult result = authService.login(
                request.username(),
                request.password(),
                clientRequestContext.userAgent(servletRequest),
                clientRequestContext.ipAddress(servletRequest)
        );
        sessionCookieWriter.writeSession(response, result);
        return result.response();
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @CookieValue(name = SessionCookieWriter.REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = SessionCookieWriter.CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        csrfTokenValidator.validate(csrfCookie, csrfHeader);
        try {
            AuthResult result = authService.refresh(
                    refreshToken,
                    clientRequestContext.userAgent(servletRequest),
                    clientRequestContext.ipAddress(servletRequest)
            );
            sessionCookieWriter.writeSession(response, result);
            return result.response();
        } catch (AuthException exception) {
            sessionCookieWriter.clearSession(response);
            throw exception;
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(NO_CONTENT)
    public void logout(
            @CookieValue(name = SessionCookieWriter.REFRESH_COOKIE, required = false) String refreshToken,
            @CookieValue(name = SessionCookieWriter.CSRF_COOKIE, required = false) String csrfCookie,
            @RequestHeader(name = "X-CSRF-Token", required = false) String csrfHeader,
            HttpServletResponse response
    ) {
        csrfTokenValidator.validate(csrfCookie, csrfHeader);
        authService.logout(refreshToken);
        sessionCookieWriter.clearSession(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal CurrentUser currentUser, HttpServletResponse response) {
        authService.logoutAll(currentUser);
        sessionCookieWriter.clearSession(response);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return authService.currentUser(currentUser);
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            String displayName
    ) {
    }

    public record LoginRequest(@NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username, @NotBlank String password) {
    }
}
