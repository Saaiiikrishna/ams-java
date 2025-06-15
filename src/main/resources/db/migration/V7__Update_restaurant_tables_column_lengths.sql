-- Update column lengths for restaurant_tables to handle longer QR codes and URLs
ALTER TABLE restaurant_tables
ALTER COLUMN qr_code TYPE VARCHAR(500);

ALTER TABLE restaurant_tables
ALTER COLUMN qr_code_url TYPE VARCHAR(2000);

-- Add soft delete column for tables
ALTER TABLE restaurant_tables
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
