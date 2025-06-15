-- Add proper indexes and constraints for better performance and data integrity

-- Add index on table number and organization for faster lookups
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_org_table_number 
ON restaurant_tables(organization_id, table_number);

-- Add index on QR code for faster QR lookups
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_qr_code 
ON restaurant_tables(qr_code);

-- Add index on orders table for table number lookups
CREATE INDEX IF NOT EXISTS idx_orders_table_number 
ON orders(table_number, organization_id);

-- Add index on orders for status filtering
CREATE INDEX IF NOT EXISTS idx_orders_status 
ON orders(status, organization_id);

-- Add constraint to ensure table numbers are positive
ALTER TABLE restaurant_tables 
ADD CONSTRAINT chk_table_number_positive 
CHECK (table_number > 0);

-- Add constraint to ensure capacity is positive if specified
ALTER TABLE restaurant_tables 
ADD CONSTRAINT chk_capacity_positive 
CHECK (capacity IS NULL OR capacity > 0);

-- Update orders table to add foreign key reference to restaurant_tables
-- First, add the table_id column
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS table_id BIGINT;

-- Add foreign key constraint (this will be nullable to support non-table orders)
ALTER TABLE orders 
ADD CONSTRAINT fk_orders_table_id 
FOREIGN KEY (table_id) REFERENCES restaurant_tables(id) 
ON DELETE SET NULL;

-- Add index for the new foreign key
CREATE INDEX IF NOT EXISTS idx_orders_table_id 
ON orders(table_id);
