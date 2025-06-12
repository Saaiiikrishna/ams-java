-- Add mobile number field to subscribers table and make email optional
-- Migration V3: Add mobile number support

-- Add mobile_number column as required field (if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='subscribers' AND column_name='mobile_number') THEN
        ALTER TABLE subscribers ADD COLUMN mobile_number VARCHAR(20) NOT NULL DEFAULT '';
    END IF;
END $$;

-- Make email column optional (remove NOT NULL constraint)
ALTER TABLE subscribers ALTER COLUMN email DROP NOT NULL;

-- Remove unique constraint on email since it's now optional
ALTER TABLE subscribers DROP CONSTRAINT IF EXISTS uk_subscribers_email;

-- Add unique constraint on mobile_number within organization
ALTER TABLE subscribers ADD CONSTRAINT uk_subscribers_mobile_org UNIQUE (mobile_number, organization_id);

-- Update any existing records to have a placeholder mobile number if needed
-- (This is for development - in production you'd want to handle this differently)
UPDATE subscribers SET mobile_number = CONCAT('TEMP_', id) WHERE mobile_number = '';
