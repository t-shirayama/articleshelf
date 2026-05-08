package com.articleshelf.domain.article;

public class TagInUseException extends RuntimeException {
    public TagInUseException(String name, long articleCount) {
        super("tag is used by " + articleCount + " article(s): " + name);
    }
}
