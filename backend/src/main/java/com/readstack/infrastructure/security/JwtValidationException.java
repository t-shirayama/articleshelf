package com.readstack.infrastructure.security;

public class JwtValidationException extends RuntimeException {
    public JwtValidationException(String message) {
        super(message);
    }
}
