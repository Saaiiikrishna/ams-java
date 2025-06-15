-- Migration V18: Increase QR code URL column size to handle base64 images
-- This migration fixes the "value too long for type character varying(2000)" error

-- Increase the qr_code_url column size to handle large base64 images
ALTER TABLE restaurant_tables ALTER COLUMN qr_code_url TYPE TEXT;

-- Add comment for documentation
COMMENT ON COLUMN restaurant_tables.qr_code_url IS 'Base64 encoded QR code image data URL (can be very large)';
