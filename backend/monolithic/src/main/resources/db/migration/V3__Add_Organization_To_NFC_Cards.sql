-- Add organization_id column to nfc_cards table
-- This establishes the relationship between NFC cards and organizations for multi-tenant support

-- Check if the column already exists to avoid errors
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'nfc_cards' AND column_name = 'organization_id'
    ) THEN
        -- Add the organization_id column (nullable initially for existing data)
        ALTER TABLE nfc_cards ADD COLUMN organization_id BIGINT;

        -- Add foreign key constraint to organizations table
        ALTER TABLE nfc_cards
        ADD CONSTRAINT fk_nfc_cards_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
        ON DELETE CASCADE;

        -- Create index for better query performance
        CREATE INDEX idx_nfc_cards_organization_id ON nfc_cards(organization_id);

        -- Create composite index for organization + assignment status queries
        CREATE INDEX idx_nfc_cards_org_subscriber ON nfc_cards(organization_id, subscriber_id);

        -- Update existing cards to belong to the first organization (if any exist)
        -- This is a temporary measure for existing data
        UPDATE nfc_cards
        SET organization_id = (
            SELECT id FROM organizations ORDER BY id LIMIT 1
        )
        WHERE organization_id IS NULL
        AND EXISTS (SELECT 1 FROM organizations);

    END IF;
END $$;
