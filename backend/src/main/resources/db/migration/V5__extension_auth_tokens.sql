CREATE TABLE IF NOT EXISTS extension_auth_codes (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(128) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    extension_id VARCHAR(64) NOT NULL,
    redirect_uri VARCHAR(512) NOT NULL,
    code_challenge VARCHAR(128) NOT NULL,
    code_challenge_method VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_extension_auth_codes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS extension_access_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    client_id VARCHAR(128) NOT NULL,
    extension_id VARCHAR(64) NOT NULL,
    scopes VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_extension_access_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_extension_auth_codes_user ON extension_auth_codes (user_id);
CREATE INDEX IF NOT EXISTS idx_extension_access_tokens_user ON extension_access_tokens (user_id);
