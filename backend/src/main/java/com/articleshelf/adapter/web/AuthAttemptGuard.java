package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthAttemptGuard {
    private final AuthRateLimiter rateLimiter;
    private final ClientRequestContext clientRequestContext;

    public AuthAttemptGuard(AuthRateLimiter rateLimiter, ClientRequestContext clientRequestContext) {
        this.rateLimiter = rateLimiter;
        this.clientRequestContext = clientRequestContext;
    }

    public void checkRegister(HttpServletRequest request) {
        rateLimiter.checkRegister(clientRequestContext.ipAddress(request));
    }

    public void checkLogin(HttpServletRequest request, String username) {
        rateLimiter.checkLogin(clientRequestContext.ipAddress(request), username);
    }
}
