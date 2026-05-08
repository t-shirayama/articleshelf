package com.articleshelf.domain.article;

public record TagUsage(
        Tag tag,
        long articleCount
) {
}
