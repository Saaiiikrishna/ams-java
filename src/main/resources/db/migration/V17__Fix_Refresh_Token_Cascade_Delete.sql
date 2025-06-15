-- Migration V17: Fix refresh token cascade delete constraints
-- This migration fixes the foreign key constraint issue preventing entity admin deletion

-- First, check if the constraint exists and drop it
DO $$
BEGIN
    -- Drop the existing foreign key constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fkons4rmu9j2eaifk83gd9crfin' 
        AND table_name = 'refresh_tokens'
    ) THEN
        ALTER TABLE refresh_tokens DROP CONSTRAINT fkons4rmu9j2eaifk83gd9crfin;
    END IF;
END $$;

-- Add the foreign key constraint with CASCADE DELETE
ALTER TABLE refresh_tokens 
ADD CONSTRAINT fk_refresh_tokens_admin_id 
FOREIGN KEY (admin_id) REFERENCES entity_admins(id) ON DELETE CASCADE;

-- Also ensure we have proper indexes
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_admin_id ON refresh_tokens(admin_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);

-- Add comments for documentation
COMMENT ON CONSTRAINT fk_refresh_tokens_admin_id ON refresh_tokens IS 'Foreign key to entity_admins with cascade delete';
COMMENT ON COLUMN refresh_tokens.admin_id IS 'References entity_admins.id, cascades on delete';
