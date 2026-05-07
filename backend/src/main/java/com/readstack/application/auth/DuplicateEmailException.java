package com.readstack.application.auth;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("メールアドレスはすでに登録されています: " + email);
    }
}
