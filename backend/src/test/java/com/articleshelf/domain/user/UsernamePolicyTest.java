package com.articleshelf.domain.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernamePolicyTest {
    private final UsernamePolicy policy = new UsernamePolicy();

    @Test
    void normalizesUsername() {
        assertThat(policy.normalize(" Reader_01 ")).isEqualTo("reader_01");
    }

    @Test
    void acceptsSupportedCharactersAndLength() {
        assertThatCode(() -> policy.validate("reader.01-test")).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidLengthAndCharacters() {
        assertThatThrownBy(() -> policy.validate("ab"))
                .isInstanceOf(UsernamePolicyException.class);

        assertThatThrownBy(() -> policy.validate("x".repeat(33)))
                .isInstanceOf(UsernamePolicyException.class);

        assertThatThrownBy(() -> policy.validate("bad username"))
                .isInstanceOf(UsernamePolicyException.class);
    }
}
