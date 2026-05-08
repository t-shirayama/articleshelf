package com.readstack.application.auth;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String username) {
        super("account was not found: " + username);
    }
}
