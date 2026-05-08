package com.readstack.domain.user;

import java.util.Locale;

public class PasswordPolicy {
    public void validate(String username, String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new PasswordPolicyException(
                    PasswordPolicyException.Reason.SIZE,
                    "password must be 8 to 128 characters long"
            );
        }
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (!normalizedUsername.isBlank() && normalizedUsername.equals(password.trim().toLowerCase(Locale.ROOT))) {
            throw new PasswordPolicyException(
                    PasswordPolicyException.Reason.SAME_AS_USERNAME,
                    "password cannot be the same as the username"
            );
        }
    }
}
