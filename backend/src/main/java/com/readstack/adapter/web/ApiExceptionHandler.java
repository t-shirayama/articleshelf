package com.readstack.adapter.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.readstack.adapter.web.AuthController.CsrfValidationException;
import com.readstack.application.auth.AuthException;
import com.readstack.application.auth.DuplicateEmailException;
import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.ArticleUrlUnavailableException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.DuplicateTagNameException;
import com.readstack.domain.article.TagInUseException;
import com.readstack.domain.article.TagNotFoundException;
import com.readstack.domain.user.PasswordPolicyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
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

    public ApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return ErrorResponse.of(message("error.article.notFound"));
    }

    @ExceptionHandler(DuplicateArticleUrlException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(DuplicateArticleUrlException exception) {
        return ErrorResponse.ofDuplicateArticle(message("error.article.duplicateUrl"), exception.getExistingArticleId());
    }

    @ExceptionHandler(DuplicateTagNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateTagName(DuplicateTagNameException exception) {
        if (exception.getReason() == DuplicateTagNameException.Reason.MERGE_TARGET_SAME) {
            return ErrorResponse.of(message("error.tag.mergeSame"));
        }
        return ErrorResponse.of(message("error.tag.duplicate"));
    }

    @ExceptionHandler(TagInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleTagInUse(TagInUseException exception) {
        return ErrorResponse.of(message("error.tag.inUse"));
    }

    @ExceptionHandler(TagNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTagNotFound(TagNotFoundException exception) {
        return ErrorResponse.of(message("error.tag.notFound"));
    }

    @ExceptionHandler(ArticleUrlUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnavailableUrl(ArticleUrlUnavailableException exception) {
        return ErrorResponse.of(message("error.article.urlUnavailable"));
    }

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuth(AuthException exception) {
        if (exception.getReason() == AuthException.Reason.INVALID_REFRESH_TOKEN) {
            return ErrorResponse.of(message("error.auth.refreshInvalid"));
        }
        return ErrorResponse.of(message("error.auth.invalidCredentials"));
    }

    @ExceptionHandler(CsrfValidationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleCsrf(RuntimeException exception) {
        return ErrorResponse.of(message("error.auth.csrf"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException exception) {
        return ErrorResponse.of(message("error.auth.duplicateEmail"));
    }

    @ExceptionHandler(PasswordPolicyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePasswordPolicy(PasswordPolicyException exception) {
        if (exception.getReason() == PasswordPolicyException.Reason.SAME_AS_EMAIL) {
            return ErrorResponse.of(message("error.auth.passwordSameAsEmail"));
        }
        return ErrorResponse.of(message("error.auth.passwordSize"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        return new ErrorResponse(Instant.now(), messages, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ErrorResponse.of(formatTypeMismatch(exception.getName(), exception.getRequiredType()));
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
            return ErrorResponse.of(formatTypeMismatch(field, invalidFormatException.getTargetType()));
        }
        return ErrorResponse.of(message("error.request.invalidBody"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ErrorResponse.of(message("error.request.internal"));
    }

    private String formatFieldError(FieldError error) {
        String field = message("field." + error.getField());
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code)) {
            return message("error.validation.required", field);
        }
        if ("Email".equals(code)) {
            return message("error.validation.email", field);
        }
        if ("Size".equals(code)) {
            return message("error.validation.size", field);
        }
        return message("error.validation.invalid", field);
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

    private Locale currentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return Locale.JAPANESE.getLanguage().equals(locale.getLanguage()) ? Locale.JAPANESE : Locale.ENGLISH;
    }

    public record ErrorResponse(Instant timestamp, List<String> messages, UUID existingArticleId) {
        static ErrorResponse of(String message) {
            return new ErrorResponse(Instant.now(), List.of(message), null);
        }

        static ErrorResponse ofDuplicateArticle(String message, UUID existingArticleId) {
            return new ErrorResponse(Instant.now(), List.of(message), existingArticleId);
        }
    }
}
