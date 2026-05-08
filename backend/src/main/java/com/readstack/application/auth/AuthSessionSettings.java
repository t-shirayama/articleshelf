package com.readstack.application.auth;

public interface AuthSessionSettings {
    long refreshTokenTtlDays();

    boolean cookieSecure();

    String cookieSameSite();

    boolean csrfEnabled();
}
