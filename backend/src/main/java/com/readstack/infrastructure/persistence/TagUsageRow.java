package com.readstack.infrastructure.persistence;

public record TagUsageRow(TagEntity tag, long articleCount) {
}
