-- Migration V11: Add scheduled sessions and enhanced attendance features
-- This migration adds support for scheduled sessions, multiple check-in methods, and subscriber authentication

-- Create scheduled_sessions table
CREATE TABLE IF NOT EXISTS scheduled_sessions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create scheduled_session_days table for days of week
CREATE TABLE IF NOT EXISTS scheduled_session_days (
    scheduled_session_id BIGINT NOT NULL REFERENCES scheduled_sessions(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL,
    PRIMARY KEY (scheduled_session_id, day_of_week)
);

-- Create scheduled_session_checkin_methods table
CREATE TABLE IF NOT EXISTS scheduled_session_checkin_methods (
    scheduled_session_id BIGINT NOT NULL REFERENCES scheduled_sessions(id) ON DELETE CASCADE,
    checkin_method VARCHAR(20) NOT NULL,
    PRIMARY KEY (scheduled_session_id, checkin_method)
);

-- Add new columns to attendance_sessions table
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS qr_code VARCHAR(500);
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS qr_code_expiry TIMESTAMP;
ALTER TABLE attendance_sessions ADD COLUMN IF NOT EXISTS scheduled_session_id BIGINT REFERENCES scheduled_sessions(id);

-- Create session_checkin_methods table for attendance sessions
CREATE TABLE IF NOT EXISTS session_checkin_methods (
    session_id BIGINT NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    checkin_method VARCHAR(20) NOT NULL,
    PRIMARY KEY (session_id, checkin_method)
);

-- Add new columns to attendance_logs table
ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS checkin_method VARCHAR(20) NOT NULL DEFAULT 'NFC';
ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS device_info TEXT;
ALTER TABLE attendance_logs ADD COLUMN IF NOT EXISTS location_info TEXT;

-- Create subscriber_auth table for mobile app authentication
CREATE TABLE IF NOT EXISTS subscriber_auth (
    id SERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL UNIQUE REFERENCES subscribers(id) ON DELETE CASCADE,
    pin VARCHAR(6) NOT NULL,
    otp_code VARCHAR(6),
    otp_expiry_time TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_device_id VARCHAR(255),
    last_device_info TEXT
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_scheduled_sessions_organization ON scheduled_sessions(organization_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_sessions_active ON scheduled_sessions(active);
CREATE INDEX IF NOT EXISTS idx_scheduled_sessions_start_time ON scheduled_sessions(start_time);

CREATE INDEX IF NOT EXISTS idx_attendance_sessions_qr_code ON attendance_sessions(qr_code);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_qr_expiry ON attendance_sessions(qr_code_expiry);
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_scheduled ON attendance_sessions(scheduled_session_id);

CREATE INDEX IF NOT EXISTS idx_attendance_logs_checkin_method ON attendance_logs(checkin_method);
CREATE INDEX IF NOT EXISTS idx_attendance_logs_device_info ON attendance_logs(device_info);

CREATE INDEX IF NOT EXISTS idx_subscriber_auth_subscriber ON subscriber_auth(subscriber_id);
CREATE INDEX IF NOT EXISTS idx_subscriber_auth_active ON subscriber_auth(is_active);
CREATE INDEX IF NOT EXISTS idx_subscriber_auth_otp_expiry ON subscriber_auth(otp_expiry_time);

-- Add some default check-in methods for existing sessions (backward compatibility)
INSERT INTO session_checkin_methods (session_id, checkin_method)
SELECT id, 'NFC' FROM attendance_sessions 
WHERE NOT EXISTS (
    SELECT 1 FROM session_checkin_methods 
    WHERE session_id = attendance_sessions.id
);
