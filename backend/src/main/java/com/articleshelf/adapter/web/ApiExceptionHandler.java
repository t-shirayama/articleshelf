package com.articleshelf.adapter.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.articleshelf.adapter.web.AuthController.CsrfValidationException;
import com.articleshelf.application.auth.AccountNotFoundException;
import com.articleshelf.application.auth.AuthException;
import com.articleshelf.application.auth.AuthRateLimitExceededException;
import com.articleshelf.application.auth.DuplicateUsernameException;
import com.articleshelf.domain.article.ArticleNotFoundException;
import com.articleshelf.domain.article.ArticleStatus;
import com.articleshelf.domain.article.ArticleUrlUnavailableException;
import com.articleshelf.domain.article.DuplicateArticleUrlException;
import com.articleshelf.domain.article.DuplicateTagNameException;
import com.articleshelf.domain.article.TagInUseException;
import com.articleshelf.domain.article.TagNotFoundException;
import com.articleshelf.domain.user.PasswordPolicyException;
import com.articleshelf.domain.user.UsernamePolicyException;
import com.articleshelf.application.observability.BackendMetrics;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final MessageSource messageSource;
    private final BackendMetrics metrics;

    public ApiExceptionHandler(MessageSource messageSource, BackendMetrics metrics) {
        this.messageSource = messageSource;
        this.metrics = metrics;
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return ErrorResponse.of("ARTICLE_NOT_FOUND", message("error.article.notFound"));
    }

    @ExceptionHandler(DuplicateArticleUrlException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(DuplicateArticleUrlException exception) {
        return ErrorResponse.ofDuplicateArticle(
                "ARTICLE_DUPLICATE_URL",
                message("error.article.duplicateUrl"),
                exception.getExistingArticleId()
        );
    }

    @ExceptionHandler(DuplicateTagNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateTagName(DuplicateTagNameException exception) {
        if (exception.getReason() == DuplicateTagNameException.Reason.MERGE_TARGET_SAME) {
            return ErrorResponse.of("TAG_MERGE_TARGET_SAME", message("error.tag.mergeSame"));
        }
        return ErrorResponse.of("TAG_DUPLICATE_NAME", message("error.tag.duplicate"));
    }

    @ExceptionHandler(TagInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleTagInUse(TagInUseException exception) {
        return ErrorResponse.of("TAG_IN_USE", message("error.tag.inUse"));
    }

    @ExceptionHandler(TagNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTagNotFound(TagNotFoundException exception) {
        return ErrorResponse.of("TAG_NOT_FOUND", message("error.tag.notFound"));
    }

    @ExceptionHandler(ArticleUrlUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnavailableUrl(ArticleUrlUnavailableException exception) {
        return ErrorResponse.of("ARTICLE_URL_UNAVAILABLE", message("error.article.urlUnavailable"));
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuth(AuthException exception) {
        if (exception.getReason() == AuthException.Reason.INVALID_REFRESH_TOKEN) {
            metrics.recordAuthFailure("refresh_invalid");
            return ErrorResponse.of("AUTH_REFRESH_INVALID", message("error.auth.refreshInvalid"));
        }
        metrics.recordAuthFailure("invalid_credentials");
        return ErrorResponse.of("AUTH_INVALID_CREDENTIALS", message("error.auth.invalidCredentials"));
    }

    @ExceptionHandler(CsrfValidationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleCsrf(RuntimeException exception) {
        return ErrorResponse.of("AUTH_CSRF_INVALID", message("error.auth.csrf"));
    }

    @ExceptionHandler(AuthRateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleAuthRateLimit(AuthRateLimitExceededException exception) {
        return ErrorResponse.of("AUTH_RATE_LIMITED", message("error.auth.rateLimited"));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateUsername(DuplicateUsernameException exception) {
        return ErrorResponse.of("AUTH_DUPLICATE_USERNAME", message("error.auth.duplicateUsername"));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFound(AccountNotFoundException exception) {
        return ErrorResponse.of("AUTH_ACCOUNT_NOT_FOUND", message("error.auth.accountNotFound"));
    }

    @ExceptionHandler(PasswordPolicyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePasswordPolicy(PasswordPolicyException exception) {
        if (exception.getReason() == PasswordPolicyException.Reason.SAME_AS_USERNAME) {
            return ErrorResponse.of("PASSWORD_SAME_AS_USERNAME", message("error.auth.passwordSameAsUsername"));
        }
        return ErrorResponse.of("PASSWORD_POLICY", message("error.auth.passwordSize"));
    }

    @ExceptionHandler(UsernamePolicyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUsernamePolicy(UsernamePolicyException exception) {
        return ErrorResponse.of("USERNAME_POLICY", message("error.auth.usernamePolicy"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        List<ErrorResponse.FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldErrorResponse(error.getField(), validationCode(error), formatFieldError(error)))
                .toList();
        return ErrorResponse.validation(messages, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ErrorResponse.of("TYPE_MISMATCH", formatTypeMismatch(exception.getName(), exception.getRequiredType()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableMessage(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFormatException.getPath().stream()
                    .findFirst()
                    .map(reference -> reference.getFieldName())
                    .filter(name -> name != null && !name.isBlank())
                    .orElse("request");
            return ErrorResponse.of("TYPE_MISMATCH", formatTypeMismatch(field, invalidFormatException.getTargetType()));
        }
        return ErrorResponse.of("INVALID_REQUEST_BODY", message("error.request.invalidBody"));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleFrameworkErrorResponse(ErrorResponseException exception) {
        return frameworkErrorResponse(exception.getStatusCode());
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleServletFrameworkException(ServletException exception) throws ServletException {
        if (exception instanceof org.springframework.web.ErrorResponse errorResponse) {
            return frameworkErrorResponse(errorResponse.getStatusCode());
        }
        throw exception;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ErrorResponse.of("INTERNAL_ERROR", message("error.request.internal"));
    }

    private String formatFieldError(FieldError error) {
        String field = message("field." + error.getField());
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return message("error.validation.required", field);
        }
        if ("Size".equals(code)) {
            return message("error.validation.size", field);
        }
        if ("Pattern".equals(code)) {
            return message("error.validation.pattern", field);
        }
        if ("URL".equals(code)) {
            return message("error.validation.url", field);
        }
        if ("Min".equals(code)) {
            return message("error.validation.min", field, constraintValue(error));
        }
        if ("Max".equals(code)) {
            return message("error.validation.max", field, constraintValue(error));
        }
        return message("error.validation.invalid", field);
    }

    private Object constraintValue(FieldError error) {
        Object[] arguments = error.getArguments();
        if (arguments == null || arguments.length < 2) {
            return "";
        }
        return arguments[1];
    }

    private String validationCode(FieldError error) {
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return "REQUIRED";
        }
        if ("Size".equals(code)) {
            return "SIZE";
        }
        if ("Pattern".equals(code)) {
            return "PATTERN";
        }
        if ("URL".equals(code)) {
            return "URL";
        }
        if ("Min".equals(code)) {
            return "MIN";
        }
        if ("Max".equals(code)) {
            return "MAX";
        }
        return "INVALID";
    }


    private String formatTypeMismatch(String field, Class<?> requiredType) {
        String fieldName = message("field." + field);
        if (requiredType == null) {
            return message("error.validation.invalid", fieldName);
        }
        if (requiredType.isEnum() && requiredType == ArticleStatus.class) {
            return message("error.type.status", fieldName);
        }
        if (requiredType == UUID.class) {
            return message("error.type.uuid", fieldName);
        }
        if (requiredType == LocalDate.class) {
            return message("error.type.date", fieldName);
        }
        if (requiredType == Boolean.class || requiredType == boolean.class) {
            return message("error.type.boolean", fieldName);
        }
        return message("error.validation.invalid", fieldName);
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, key, currentLocale());
    }

    private ResponseEntity<ErrorResponse> frameworkErrorResponse(HttpStatusCode statusCode) {
        String key = statusCode.is4xxClientError() ? "error.request.invalid" : "error.request.internal";
        String code = statusCode.is4xxClientError() ? "REQUEST_INVALID" : "INTERNAL_ERROR";
        return ResponseEntity.status(statusCode).body(ErrorResponse.of(code, message(key)));
    }

    private Locale currentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return Locale.JAPANESE.getLanguage().equals(locale.getLanguage()) ? Locale.JAPANESE : Locale.ENGLISH;
    }

    public record ErrorResponse(
            Instant timestamp,
            String code,
            List<String> messages,
            List<FieldErrorResponse> fieldErrors,
            UUID existingArticleId
    ) {
        static ErrorResponse of(String code, String message) {
            return new ErrorResponse(Instant.now(), code, List.of(message), List.of(), null);
        }

        static ErrorResponse ofDuplicateArticle(String code, String message, UUID existingArticleId) {
            return new ErrorResponse(Instant.now(), code, List.of(message), List.of(), existingArticleId);
        }

        static ErrorResponse validation(List<String> messages, List<FieldErrorResponse> fieldErrors) {
            return new ErrorResponse(Instant.now(), "VALIDATION_ERROR", messages, fieldErrors, null);
        }

        public record FieldErrorResponse(String field, String code, String message) {
        }
    }
}
