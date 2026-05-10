package com.articleshelf.application.observability;

import java.time.Duration;

public interface BackendMetrics {
    void recordArticleCreated();

    void recordArticleUpdated();

    void recordAuthFailure(String reason);

    void recordAuthRateLimited(String operation);

    void recordOgpFetch(Duration duration, String outcome);

    static BackendMetrics noop() {
        return new BackendMetrics() {
            @Override
            public void recordArticleCreated() {
            }

            @Override
            public void recordArticleUpdated() {
            }

            @Override
            public void recordAuthFailure(String reason) {
            }

            @Override
            public void recordAuthRateLimited(String operation) {
            }

            @Override
            public void recordOgpFetch(Duration duration, String outcome) {
            }
        };
    }
}
