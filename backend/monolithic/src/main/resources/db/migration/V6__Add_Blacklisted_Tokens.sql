-- Create blacklisted_tokens table for JWT token security
CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    id SERIAL PRIMARY KEY,
    token_hash VARCHAR(1024) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR(100)
);

-- Create index for fast token lookup
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_hash ON blacklisted_tokens(token_hash);

-- Create index for cleanup operations
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_expires_at ON blacklisted_tokens(expires_at);

-- Create index for user-based operations
CREATE INDEX IF NOT EXISTS idx_blacklisted_tokens_username ON blacklisted_tokens(username);
