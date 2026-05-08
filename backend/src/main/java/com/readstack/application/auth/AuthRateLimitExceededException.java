package com.readstack.application.auth;

public class AuthRateLimitExceededException extends RuntimeException {
    public AuthRateLimitExceededException(String message) {
        super(message);
    }
}
