-- Fix the database issues manually
-- This script fixes the QR code column size issue

-- Fix the qr_code_url column size
ALTER TABLE restaurant_tables ALTER COLUMN qr_code_url TYPE TEXT;

-- Check if the column was updated
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'restaurant_tables' AND column_name = 'qr_code_url';
