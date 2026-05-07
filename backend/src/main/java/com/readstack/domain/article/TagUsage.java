package com.readstack.domain.article;

public record TagUsage(
        Tag tag,
        long articleCount
) {
}
