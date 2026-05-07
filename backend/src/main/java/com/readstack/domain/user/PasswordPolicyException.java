package com.readstack.domain.user;

public class PasswordPolicyException extends RuntimeException {
    public PasswordPolicyException(String message) {
        super(message);
    }
}
