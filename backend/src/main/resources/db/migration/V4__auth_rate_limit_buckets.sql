CREATE TABLE IF NOT EXISTS auth_rate_limit_buckets (
    bucket_key VARCHAR(256) PRIMARY KEY,
    operation VARCHAR(32) NOT NULL,
    tokens INTEGER NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_rate_limit_buckets_updated_at
    ON auth_rate_limit_buckets (updated_at);
