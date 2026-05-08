package com.articleshelf.application.auth;

public class AuthException extends RuntimeException {
    public enum Reason {
        INVALID_CREDENTIALS,
        INVALID_REFRESH_TOKEN,
        USER_INACTIVE
    }

    private final Reason reason;

    public AuthException(String message) {
        this(Reason.INVALID_CREDENTIALS, message);
    }

    public AuthException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static AuthException invalidRefreshToken(String message) {
        return new AuthException(Reason.INVALID_REFRESH_TOKEN, message);
    }

    public Reason getReason() {
        return reason;
    }
}
