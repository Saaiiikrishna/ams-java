-- Create super_admin_refresh_tokens table
CREATE TABLE IF NOT EXISTS super_admin_refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(1024) NOT NULL UNIQUE,
    super_admin_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (super_admin_id) REFERENCES super_admins(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_super_admin_refresh_tokens_token ON super_admin_refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_super_admin_refresh_tokens_super_admin_id ON super_admin_refresh_tokens(super_admin_id);
CREATE INDEX IF NOT EXISTS idx_super_admin_refresh_tokens_expiry_date ON super_admin_refresh_tokens(expiry_date);
