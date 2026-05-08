package com.readstack.adapter.web;

import com.readstack.application.auth.AuthException;
import com.readstack.application.auth.AuthRateLimitExceededException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.DuplicateTagNameException;
import com.readstack.domain.user.PasswordPolicyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        handler = new ApiExceptionHandler(messageSource);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void duplicateArticleUrlResponseIncludesExistingArticleId() {
        UUID existingArticleId = UUID.randomUUID();

        ApiExceptionHandler.ErrorResponse response = handler.handleConflict(
                new DuplicateArticleUrlException("https://example.com/article", existingArticleId)
        );

        assertThat(response.messages()).containsExactly("This URL is already registered.");
        assertThat(response.existingArticleId()).isEqualTo(existingArticleId);
    }

    @Test
    void duplicateTagResponsesUseReasonCodeInsteadOfExceptionMessage() {
        ApiExceptionHandler.ErrorResponse duplicateName = handler.handleDuplicateTagName(
                new DuplicateTagNameException("Vue")
        );
        ApiExceptionHandler.ErrorResponse mergeSame = handler.handleDuplicateTagName(
                DuplicateTagNameException.mergeTargetSame()
        );

        assertThat(duplicateName.messages()).containsExactly("Tag already exists.");
        assertThat(mergeSame.messages()).containsExactly("Choose a different tag to merge into.");
    }

    @Test
    void authResponsesUseReasonCodeInsteadOfExceptionMessage() {
        ApiExceptionHandler.ErrorResponse invalidCredentials = handler.handleAuth(
                new AuthException(AuthException.Reason.INVALID_CREDENTIALS, "localized text should not be inspected")
        );
        ApiExceptionHandler.ErrorResponse invalidRefresh = handler.handleAuth(
                new AuthException(AuthException.Reason.INVALID_REFRESH_TOKEN, "missing session")
        );
        ApiExceptionHandler.ErrorResponse inactiveUser = handler.handleAuth(
                new AuthException(AuthException.Reason.USER_INACTIVE, "user is not active")
        );

        assertThat(invalidCredentials.messages()).containsExactly("Username or password is incorrect.");
        assertThat(invalidRefresh.messages()).containsExactly("Session is invalid. Please log in again.");
        assertThat(inactiveUser.messages()).containsExactly("Username or password is incorrect.");
    }

    @Test
    void authRateLimitResponseUsesGenericMessage() {
        ApiExceptionHandler.ErrorResponse response = handler.handleAuthRateLimit(
                new AuthRateLimitExceededException("internal key should not leak")
        );

        assertThat(response.messages()).containsExactly("Too many authentication attempts. Please wait and try again.");
    }

    @Test
    void passwordPolicyResponsesUseReasonCodeInsteadOfExceptionMessage() {
        ApiExceptionHandler.ErrorResponse invalidSize = handler.handlePasswordPolicy(
                new PasswordPolicyException(PasswordPolicyException.Reason.SIZE, "too short")
        );
        ApiExceptionHandler.ErrorResponse sameAsUsername = handler.handlePasswordPolicy(
                new PasswordPolicyException(PasswordPolicyException.Reason.SAME_AS_USERNAME, "same value")
        );

        assertThat(invalidSize.messages()).containsExactly("Password must be 8 to 128 characters long.");
        assertThat(sameAsUsername.messages()).containsExactly("Password cannot be the same as the username.");
    }

    @Test
    void unexpectedErrorsReturnGenericMessageWithoutLeakingExceptionDetails() {
        ApiExceptionHandler.ErrorResponse response = handler.handleUnexpected(
                new IllegalStateException("internal implementation detail")
        );

        assertThat(response.messages()).containsExactly("An unexpected error occurred.");
        assertThat(response.messages().get(0)).doesNotContain("internal implementation detail");
    }
}
