-- Migration V14: Add checkout method tracking and enforce one check-in per subscriber per session
-- This migration adds separate check-out method tracking and database constraints

-- Add checkout_method column to attendance_logs table
ALTER TABLE attendance_logs 
ADD COLUMN IF NOT EXISTS checkout_method VARCHAR(50);

-- Add comment to clarify the column purpose
COMMENT ON COLUMN attendance_logs.checkout_method IS 'Method used for check-out (NFC, QR, WiFi, Bluetooth, Mobile NFC, Manual)';

-- Add unique constraint to enforce one attendance record per subscriber per session
-- This prevents multiple check-ins for the same subscriber in the same session
ALTER TABLE attendance_logs 
ADD CONSTRAINT IF NOT EXISTS uk_subscriber_session 
UNIQUE (subscriber_id, session_id);

-- Update existing records to set checkout_method same as checkin_method where checkout_time exists
-- This is for backward compatibility with existing data
UPDATE attendance_logs 
SET checkout_method = checkin_method 
WHERE checkout_time IS NOT NULL AND checkout_method IS NULL;

-- Add index for better performance on common queries
CREATE INDEX IF NOT EXISTS idx_attendance_logs_subscriber_session 
ON attendance_logs(subscriber_id, session_id);

CREATE INDEX IF NOT EXISTS idx_attendance_logs_checkout_time 
ON attendance_logs(checkout_time);

CREATE INDEX IF NOT EXISTS idx_attendance_logs_methods 
ON attendance_logs(checkin_method, checkout_method);
