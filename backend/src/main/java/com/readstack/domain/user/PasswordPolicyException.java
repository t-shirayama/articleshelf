package com.readstack.domain.user;

public class PasswordPolicyException extends RuntimeException {
    public enum Reason {
        SIZE,
        SAME_AS_EMAIL
    }

    private final Reason reason;

    public PasswordPolicyException(String message) {
        this(Reason.SIZE, message);
    }

    public PasswordPolicyException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
