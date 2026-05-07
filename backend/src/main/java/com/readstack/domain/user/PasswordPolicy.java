package com.readstack.domain.user;

import java.util.Locale;

public class PasswordPolicy {
    public void validate(String email, String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new PasswordPolicyException("パスワードは8文字以上128文字以下で入力してください");
        }
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.isBlank() && normalizedEmail.equals(password.trim().toLowerCase(Locale.ROOT))) {
            throw new PasswordPolicyException("メールアドレスと同じパスワードは使用できません");
        }
    }
}
