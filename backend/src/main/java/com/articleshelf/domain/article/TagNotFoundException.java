package com.articleshelf.domain.article;

import java.util.UUID;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(UUID id) {
        super("tag not found: " + id);
    }
}
