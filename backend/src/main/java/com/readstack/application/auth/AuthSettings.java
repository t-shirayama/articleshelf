package com.readstack.application.auth;

public interface AuthSettings {
    long refreshTokenTtlDays();

    String initialUsername();

    String initialUserPassword();
}
