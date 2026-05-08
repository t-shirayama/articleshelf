package com.articleshelf.domain.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithinLengthAndDifferentFromUsername() {
        assertThatCode(() -> policy.validate("reader", "secure-password"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTooShortTooLongAndUsernameEquivalentPassword() {
        assertThatThrownBy(() -> policy.validate("reader", "short"))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SIZE));

        assertThatThrownBy(() -> policy.validate("reader", "x".repeat(129)))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SIZE));

        assertThatThrownBy(() -> policy.validate("Reader", " reader "))
                .isInstanceOf(PasswordPolicyException.class)
                .satisfies(exception -> assertThat(((PasswordPolicyException) exception).getReason())
                        .isEqualTo(PasswordPolicyException.Reason.SAME_AS_USERNAME));
    }
}
