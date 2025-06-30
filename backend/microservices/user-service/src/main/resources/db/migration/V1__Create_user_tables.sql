-- User Service Database Migration
-- Creates unified user tables for all user types: SUPER_ADMIN, ENTITY_ADMIN, MEMBER

-- Create users table (unified table for all user types)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    mobile_number VARCHAR(15),
    user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('SUPER_ADMIN', 'ENTITY_ADMIN', 'MEMBER')),
    organization_id BIGINT REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    last_device_id VARCHAR(255),
    last_device_info TEXT,
    last_login_time TIMESTAMP,
    otp_code VARCHAR(6),
    otp_expiry_time TIMESTAMP
);

-- Create user_permissions table for fine-grained access control
CREATE TABLE IF NOT EXISTS user_permissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    granted BOOLEAN DEFAULT TRUE,
    granted_by BIGINT,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_mobile_number ON users(mobile_number);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
CREATE INDEX IF NOT EXISTS idx_users_created_by ON users(created_by);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_permission ON user_permissions(permission);
CREATE INDEX IF NOT EXISTS idx_user_permissions_granted ON user_permissions(granted);
CREATE INDEX IF NOT EXISTS idx_user_permissions_expires_at ON user_permissions(expires_at);

-- Create composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_users_type_org ON users(user_type, organization_id);
CREATE INDEX IF NOT EXISTS idx_users_type_active ON users(user_type, is_active);
CREATE INDEX IF NOT EXISTS idx_users_org_active ON users(organization_id, is_active);
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_permission ON user_permissions(user_id, permission);

-- Add constraints
ALTER TABLE users ADD CONSTRAINT check_super_admin_no_org 
    CHECK (
        (user_type = 'SUPER_ADMIN' AND organization_id IS NULL) OR
        (user_type IN ('ENTITY_ADMIN', 'MEMBER') AND organization_id IS NOT NULL)
    );

ALTER TABLE users ADD CONSTRAINT check_member_has_mobile 
    CHECK (
        (user_type = 'MEMBER' AND mobile_number IS NOT NULL) OR
        (user_type IN ('SUPER_ADMIN', 'ENTITY_ADMIN'))
    );

-- Add unique constraint for mobile number (for members)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_mobile_unique 
    ON users(mobile_number) 
    WHERE mobile_number IS NOT NULL;

-- Add unique constraint for email (optional but unique if provided)
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_unique 
    ON users(email) 
    WHERE email IS NOT NULL;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default super admin if not exists
-- Password is 'password' encoded with BCrypt
INSERT INTO users (
    username, 
    password, 
    email, 
    first_name, 
    last_name, 
    user_type, 
    organization_id, 
    is_active,
    created_by
) VALUES (
    'superadmin',
    '$2a$10$kX6/9Z0SqnX5nGooy2cuglRez2oJ6TP2PzefDs2fzGEGZm4G6dkhO',
    'superadmin@example.com',
    'Super',
    'Admin',
    'SUPER_ADMIN',
    NULL,
    TRUE,
    NULL
) ON CONFLICT (username) DO NOTHING;

-- Grant default permissions to super admin
INSERT INTO user_permissions (user_id, permission, granted, granted_by, granted_at)
SELECT 
    u.id,
    perm.permission,
    TRUE,
    NULL,
    CURRENT_TIMESTAMP
FROM users u
CROSS JOIN (
    VALUES 
    ('SYSTEM_ADMIN'),
    ('CREATE_SUPER_ADMIN'),
    ('MANAGE_SUPER_ADMINS'),
    ('SYSTEM_SETTINGS'),
    ('SYSTEM_MONITORING'),
    ('CREATE_ENTITY'),
    ('MANAGE_ENTITIES'),
    ('DELETE_ENTITIES'),
    ('CREATE_ENTITY_ADMIN'),
    ('MANAGE_ENTITY_ADMINS'),
    ('DELETE_ENTITY_ADMIN'),
    ('ASSIGN_ENTITY_ADMIN'),
    ('ACCESS_ALL_ENTITIES'),
    ('GLOBAL_REPORTS'),
    ('GLOBAL_ANALYTICS'),
    ('NFC_SIMULATION'),
    ('CHANGE_PASSWORD'),
    ('UPDATE_PROFILE')
) AS perm(permission)
WHERE u.username = 'superadmin' 
AND u.user_type = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

-- Create view for active users by type
CREATE OR REPLACE VIEW active_users_by_type AS
SELECT 
    user_type,
    organization_id,
    COUNT(*) as user_count
FROM users 
WHERE is_active = TRUE 
GROUP BY user_type, organization_id;

-- Create view for user permissions summary
CREATE OR REPLACE VIEW user_permissions_summary AS
SELECT 
    u.id as user_id,
    u.username,
    u.user_type,
    u.organization_id,
    COUNT(up.id) as total_permissions,
    COUNT(CASE WHEN up.granted = TRUE THEN 1 END) as granted_permissions,
    COUNT(CASE WHEN up.granted = TRUE AND (up.expires_at IS NULL OR up.expires_at > CURRENT_TIMESTAMP) THEN 1 END) as active_permissions
FROM users u
LEFT JOIN user_permissions up ON u.id = up.user_id
GROUP BY u.id, u.username, u.user_type, u.organization_id;

-- Add comments for documentation
COMMENT ON TABLE users IS 'Unified user table for all user types: SUPER_ADMIN, ENTITY_ADMIN, MEMBER';
COMMENT ON TABLE user_permissions IS 'Fine-grained permissions for users with expiration support';

COMMENT ON COLUMN users.user_type IS 'Type of user: SUPER_ADMIN, ENTITY_ADMIN, or MEMBER';
COMMENT ON COLUMN users.organization_id IS 'NULL for SUPER_ADMIN, required for ENTITY_ADMIN and MEMBER';
COMMENT ON COLUMN users.mobile_number IS 'Required for MEMBER type, used as username for mobile authentication';
COMMENT ON COLUMN users.created_by IS 'ID of the user who created this user';
COMMENT ON COLUMN users.otp_code IS 'Temporary OTP for mobile authentication';

COMMENT ON COLUMN user_permissions.permission IS 'Permission name from Permission enum';
COMMENT ON COLUMN user_permissions.granted IS 'Whether permission is granted or revoked';
COMMENT ON COLUMN user_permissions.expires_at IS 'Optional expiration time for temporary permissions';

-- Create function to check user permission
CREATE OR REPLACE FUNCTION check_user_permission(
    p_user_id BIGINT,
    p_permission VARCHAR(100)
) RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 
        FROM user_permissions 
        WHERE user_id = p_user_id 
        AND permission = p_permission 
        AND granted = TRUE 
        AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
    );
END;
$$ LANGUAGE plpgsql;

-- Create function to get user permissions
CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id BIGINT)
RETURNS TABLE(permission VARCHAR(100)) AS $$
BEGIN
    RETURN QUERY
    SELECT up.permission
    FROM user_permissions up
    WHERE up.user_id = p_user_id 
    AND up.granted = TRUE 
    AND (up.expires_at IS NULL OR up.expires_at > CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;
