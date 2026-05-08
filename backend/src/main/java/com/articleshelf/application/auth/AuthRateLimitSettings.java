package com.articleshelf.application.auth;

public interface AuthRateLimitSettings {
    boolean enabled();

    int loginCapacity();

    long loginWindowSeconds();

    int registerCapacity();

    long registerWindowSeconds();
}
