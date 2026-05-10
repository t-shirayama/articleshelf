package com.articleshelf.adapter.observability;

import com.articleshelf.application.observability.BackendMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MicrometerBackendMetrics implements BackendMetrics {
    private final MeterRegistry meterRegistry;

    public MicrometerBackendMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordArticleCreated() {
        meterRegistry.counter("articleshelf.article.created").increment();
    }

    @Override
    public void recordArticleUpdated() {
        meterRegistry.counter("articleshelf.article.updated").increment();
    }

    @Override
    public void recordAuthFailure(String reason) {
        meterRegistry.counter("articleshelf.auth.failure", "reason", reason).increment();
    }

    @Override
    public void recordAuthRateLimited(String operation) {
        meterRegistry.counter("articleshelf.auth.rate_limited", "operation", operation).increment();
    }

    @Override
    public void recordOgpFetch(Duration duration, String outcome) {
        meterRegistry.timer("articleshelf.ogp.fetch", "outcome", outcome).record(duration);
    }
}
