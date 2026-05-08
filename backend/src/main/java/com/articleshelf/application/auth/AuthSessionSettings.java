package com.articleshelf.application.auth;

public interface AuthSessionSettings {
    long refreshTokenTtlDays();

    boolean cookieSecure();

    String cookieSameSite();

    boolean csrfEnabled();
}
