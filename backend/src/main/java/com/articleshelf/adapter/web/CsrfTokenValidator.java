package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthSessionSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class CsrfTokenValidator {
    private final AuthSessionSettings settings;

    public CsrfTokenValidator(AuthSessionSettings settings) {
        this.settings = settings;
    }

    public void validate(String csrfCookie, String csrfHeader) {
        if (!settings.csrfEnabled()) {
            return;
        }
        if (csrfCookie == null || csrfCookie.isBlank() || !csrfCookie.equals(csrfHeader)) {
            throw new CsrfValidationException();
        }
    }

    public void validate(HttpServletRequest request) {
        validate(readCsrfCookie(request), request.getHeader("X-CSRF-Token"));
    }

    private String readCsrfCookie(HttpServletRequest request) {
        var csrfCookie = WebUtils.getCookie(request, SessionCookieWriter.CSRF_COOKIE);
        return csrfCookie == null ? null : csrfCookie.getValue();
    }
}
