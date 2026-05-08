package com.readstack.domain.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithinLengthAndDifferentFromEmail() {
        assertThatCode(() -> policy.validate("user@example.com", "secure-password"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortTooLongAndEmailEquivalentPassword() {
        assertThatThrownBy(() -> policy.validate("user@example.com", "short"))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SIZE));

        assertThatThrownBy(() -> policy.validate("user@example.com", "x".repeat(129)))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SIZE));

        assertThatThrownBy(() -> policy.validate("User@Example.com", " user@example.com "))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SAME_AS_EMAIL));
    }
}
