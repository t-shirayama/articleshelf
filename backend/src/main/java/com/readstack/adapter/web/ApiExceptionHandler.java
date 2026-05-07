package com.readstack.adapter.web;

import com.readstack.domain.article.ArticleNotFoundException;
import com.readstack.domain.article.ArticleStatus;
import com.readstack.domain.article.ArticleUrlUnavailableException;
import com.readstack.domain.article.DuplicateArticleUrlException;
import com.readstack.domain.article.DuplicateTagNameException;
import com.readstack.domain.article.TagInUseException;
import com.readstack.domain.article.TagNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.readstack.adapter.web.AuthController.CsrfValidationException;
import com.readstack.application.auth.AuthException;
import com.readstack.application.auth.DuplicateEmailException;
import com.readstack.domain.user.PasswordPolicyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ArticleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(RuntimeException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(DuplicateArticleUrlException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(DuplicateArticleUrlException exception) {
        return ErrorResponse.ofDuplicateArticle(exception.getMessage(), exception.getExistingArticleId());
    }

    @ExceptionHandler(DuplicateTagNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateTagName(DuplicateTagNameException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(TagInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleTagInUse(TagInUseException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(TagNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTagNotFound(TagNotFoundException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(ArticleUrlUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnavailableUrl(ArticleUrlUnavailableException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler({AuthException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuth(RuntimeException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(CsrfValidationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleCsrf(RuntimeException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException exception) {
        return ErrorResponse.of(exception.getMessage());
    }

    @ExceptionHandler(PasswordPolicyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePasswordPolicy(PasswordPolicyException exception) {
        return ErrorResponse.of(exception.getMessage());
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
        return ErrorResponse.of("request body is invalid");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private String formatTypeMismatch(String field, Class<?> requiredType) {
        if (requiredType == null) {
            return field + " is invalid";
        }
        if (requiredType.isEnum() && requiredType == ArticleStatus.class) {
            return field + " must be one of: UNREAD, READ";
        }
        if (requiredType == UUID.class) {
            return field + " must be a valid UUID";
        }
        if (requiredType == LocalDate.class) {
            return field + " must be a valid date in yyyy-MM-dd format";
        }
        if (requiredType == Boolean.class || requiredType == boolean.class) {
            return field + " must be true or false";
        }
        return field + " is invalid";
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
