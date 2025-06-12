-- Fix subscriber_auth pin column length to accommodate encoded passwords
-- Migration V13: Update pin column length from 6 to 255 characters

-- Update the pin column to allow for encoded passwords
ALTER TABLE subscriber_auth ALTER COLUMN pin TYPE VARCHAR(255);

-- Add comment to clarify the column purpose
COMMENT ON COLUMN subscriber_auth.pin IS 'Encoded PIN for mobile app access (BCrypt encoded)';
