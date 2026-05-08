package com.articleshelf.application.auth;

public interface AccessTokenIssuer {
    String issue(CurrentUser user);
}
