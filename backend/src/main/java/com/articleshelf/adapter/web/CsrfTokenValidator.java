package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthSessionSettings;
import org.springframework.stereotype.Component;

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
}
