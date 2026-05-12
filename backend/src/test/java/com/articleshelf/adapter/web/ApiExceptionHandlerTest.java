package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.AuthException;
import com.articleshelf.application.auth.AuthRateLimitExceededException;
import com.articleshelf.domain.article.ArticleVersionConflictException;
import com.articleshelf.domain.article.DuplicateArticleUrlException;
import com.articleshelf.domain.article.DuplicateTagNameException;
import com.articleshelf.domain.user.PasswordPolicyException;
import com.articleshelf.application.observability.BackendMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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
        handler = new ApiExceptionHandler(messageSource, BackendMetrics.noop());
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

        assertThat(response.code()).isEqualTo("ARTICLE_DUPLICATE_URL");
        assertThat(response.messages()).containsExactly("This URL is already registered.");
        assertThat(response.fieldErrors()).isEmpty();
        assertThat(response.existingArticleId()).isEqualTo(existingArticleId);
    }

    @Test
    void articleVersionConflictUsesMachineReadableConflictCode() {
        ApiExceptionHandler.ErrorResponse response = handler.handleVersionConflict(
                new ArticleVersionConflictException(UUID.randomUUID())
        );

        assertThat(response.code()).isEqualTo("ARTICLE_VERSION_CONFLICT");
        assertThat(response.messages()).containsExactly("This article was updated in another tab or device. Reload the latest content, then apply your changes again.");
        assertThat(response.existingArticleId()).isNull();
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
        assertThat(duplicateName.code()).isEqualTo("TAG_DUPLICATE_NAME");
        assertThat(mergeSame.messages()).containsExactly("Choose a different tag to merge into.");
        assertThat(mergeSame.code()).isEqualTo("TAG_MERGE_TARGET_SAME");
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
        assertThat(invalidCredentials.code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(invalidRefresh.messages()).containsExactly("Session is invalid. Please log in again.");
        assertThat(invalidRefresh.code()).isEqualTo("AUTH_REFRESH_INVALID");
        assertThat(inactiveUser.messages()).containsExactly("Username or password is incorrect.");
        assertThat(inactiveUser.code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void authRateLimitResponseUsesGenericMessage() {
        ApiExceptionHandler.ErrorResponse response = handler.handleAuthRateLimit(
                new AuthRateLimitExceededException("internal key should not leak")
        );

        assertThat(response.messages()).containsExactly("Too many authentication attempts. Please wait and try again.");
        assertThat(response.code()).isEqualTo("AUTH_RATE_LIMITED");
    }

    @Test
    void validationMessagesHandleUrlMinAndMaxSpecifically() {
        assertThat(formatFieldError("url", "URL", null))
                .isEqualTo("URL must be a valid URL.");
        assertThat(formatFieldError("rating", "Min", 0))
                .isEqualTo("Rating must be at least 0.");
        assertThat(formatFieldError("rating", "Max", 5))
                .isEqualTo("Rating must be at most 5.");
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
        assertThat(invalidSize.code()).isEqualTo("PASSWORD_POLICY");
        assertThat(sameAsUsername.messages()).containsExactly("Password cannot be the same as the username.");
        assertThat(sameAsUsername.code()).isEqualTo("PASSWORD_SAME_AS_USERNAME");
    }

    @Test
    void frameworkClientErrorsKeepTheirOriginalStatusCode() throws Exception {
        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = handler.handleServletFrameworkException(
                new HttpRequestMethodNotSupportedException("GET")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(405);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().messages()).containsExactly("Request is invalid.");
        assertThat(response.getBody().code()).isEqualTo("REQUEST_INVALID");
    }

    @Test
    void unexpectedErrorsReturnGenericMessageWithoutLeakingExceptionDetails() {
        ApiExceptionHandler.ErrorResponse response = handler.handleUnexpected(
                new IllegalStateException("internal implementation detail")
        );

        assertThat(response.messages()).containsExactly("An unexpected error occurred.");
        assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.messages().get(0)).doesNotContain("internal implementation detail");
    }

    @Test
    void validationResponseIncludesMachineReadableFieldErrors() throws Exception {
        FieldError fieldError = new FieldError(
                "request",
                "url",
                null,
                false,
                new String[]{"URL"},
                null,
                null
        );
        MethodArgumentNotValidExceptionFixture exception = new MethodArgumentNotValidExceptionFixture(fieldError);

        ApiExceptionHandler.ErrorResponse response = handler.handleValidation(exception);

        assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.messages()).containsExactly("URL must be a valid URL.");
        assertThat(response.fieldErrors()).singleElement().satisfies(error -> {
            assertThat(error.field()).isEqualTo("url");
            assertThat(error.code()).isEqualTo("URL");
            assertThat(error.message()).isEqualTo("URL must be a valid URL.");
        });
    }

    private String formatFieldError(String field, String code, Object constraintValue) {
        Object[] arguments = constraintValue == null ? null : new Object[]{field, constraintValue};
        FieldError error = new FieldError("request", field, null, false, new String[]{code}, arguments, null);
        return ReflectionTestUtils.invokeMethod(handler, "formatFieldError", error);
    }

    private static class MethodArgumentNotValidExceptionFixture extends org.springframework.web.bind.MethodArgumentNotValidException {
        MethodArgumentNotValidExceptionFixture(FieldError fieldError) throws NoSuchMethodException {
            super(
                    new org.springframework.core.MethodParameter(
                            MethodArgumentNotValidExceptionFixture.class.getDeclaredMethod("request", Object.class),
                            0
                    ),
                    new org.springframework.validation.BeanPropertyBindingResult(new Object(), "request")
            );
            getBindingResult().addError(fieldError);
        }

        @SuppressWarnings("unused")
        private void request(Object request) {
        }
    }
}
