package com.readstack.application.auth;

public interface AuthSettings {
    long refreshTokenTtlDays();

    String initialUserEmail();

    String initialUserPassword();
}
