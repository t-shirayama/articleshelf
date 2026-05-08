package com.articleshelf.infrastructure.persistence;

public record TagUsageRow(TagEntity tag, long articleCount) {
}
