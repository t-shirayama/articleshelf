package com.articleshelf.application.auth;

public interface AuthSettings {
    long refreshTokenTtlDays();

    boolean initialUserEnabled();

    String initialUsername();

    String initialUserPassword();
}
