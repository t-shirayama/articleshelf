package com.readstack.domain.article;

public class DuplicateTagNameException extends RuntimeException {
    public DuplicateTagNameException(String name) {
        super("tag already exists: " + name);
    }
}
