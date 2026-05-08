package com.articleshelf.application.auth;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("username is already registered: " + username);
    }
}
