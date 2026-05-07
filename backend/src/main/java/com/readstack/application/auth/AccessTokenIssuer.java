package com.readstack.application.auth;

public interface AccessTokenIssuer {
    String issue(CurrentUser user);
}
