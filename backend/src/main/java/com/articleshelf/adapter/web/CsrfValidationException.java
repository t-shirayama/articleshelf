package com.articleshelf.adapter.web;

public class CsrfValidationException extends RuntimeException {
    public CsrfValidationException() {
        super("CSRF token is invalid");
    }
}
