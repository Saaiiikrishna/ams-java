-- Change NFC Cards to use Entity ID as foreign key instead of organization primary key
-- This migration changes the foreign key relationship to reference entity_id instead of id

-- Step 1: Add the new entity_id column that will reference organizations.entity_id
ALTER TABLE nfc_cards ADD COLUMN IF NOT EXISTS entity_id VARCHAR(8);

-- Step 2: Populate entity_id from existing organization relationships
UPDATE nfc_cards 
SET entity_id = (
    SELECT o.entity_id 
    FROM organizations o 
    WHERE o.id = nfc_cards.organization_id
)
WHERE nfc_cards.organization_id IS NOT NULL;

-- Step 3: Drop the old foreign key constraint
ALTER TABLE nfc_cards DROP CONSTRAINT IF EXISTS fk_nfc_cards_organization;

-- Step 4: Drop the old indexes
DROP INDEX IF EXISTS idx_nfc_cards_organization_id;
DROP INDEX IF EXISTS idx_nfc_cards_org_subscriber;
DROP INDEX IF EXISTS idx_nfc_cards_organization_entity_id;

-- Step 5: Drop the old organization_id column (ensure it's completely removed)
ALTER TABLE nfc_cards DROP COLUMN IF EXISTS organization_id CASCADE;

-- Step 6: Create new foreign key constraint referencing entity_id
ALTER TABLE nfc_cards 
ADD CONSTRAINT fk_nfc_cards_entity_id 
FOREIGN KEY (entity_id) REFERENCES organizations(entity_id) 
ON DELETE CASCADE;

-- Step 7: Create indexes for the new entity_id column
CREATE INDEX IF NOT EXISTS idx_nfc_cards_entity_id ON nfc_cards(entity_id);

-- Step 8: Create composite index for entity_id + assignment status queries
CREATE INDEX IF NOT EXISTS idx_nfc_cards_entity_subscriber ON nfc_cards(entity_id, subscriber_id);

-- Step 9: Verify organization_id column is completely removed
DO $$
BEGIN
    -- Check if organization_id column still exists
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'nfc_cards'
        AND column_name = 'organization_id'
    ) THEN
        RAISE EXCEPTION 'ERROR: organization_id column still exists in nfc_cards table!';
    END IF;

    -- Verify entity_id column exists and has foreign key
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'nfc_cards'
        AND column_name = 'entity_id'
    ) THEN
        RAISE EXCEPTION 'ERROR: entity_id column does not exist in nfc_cards table!';
    END IF;

    RAISE NOTICE 'SUCCESS: organization_id column successfully removed from nfc_cards table';
END $$;

-- Step 10: Log the migration results
DO $$
DECLARE
    total_cards INTEGER;
    cards_with_entity_id INTEGER;
    cards_without_entity_id INTEGER;
    orphaned_cards INTEGER;
BEGIN
    -- Count total cards
    SELECT COUNT(*) INTO total_cards FROM nfc_cards;

    -- Count cards with entity_id
    SELECT COUNT(*) INTO cards_with_entity_id FROM nfc_cards WHERE entity_id IS NOT NULL;

    -- Count cards without entity_id
    SELECT COUNT(*) INTO cards_without_entity_id FROM nfc_cards WHERE entity_id IS NULL;

    -- Count orphaned cards (cards with entity_id that don't match any organization)
    SELECT COUNT(*) INTO orphaned_cards
    FROM nfc_cards n
    WHERE n.entity_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1 FROM organizations o WHERE o.entity_id = n.entity_id
    );

    RAISE NOTICE 'NFC Cards Entity ID Foreign Key Migration Complete:';
    RAISE NOTICE '- Total cards: %', total_cards;
    RAISE NOTICE '- Cards with Entity ID: %', cards_with_entity_id;
    RAISE NOTICE '- Cards without Entity ID: %', cards_without_entity_id;
    RAISE NOTICE '- Orphaned cards (invalid Entity ID): %', orphaned_cards;

    IF cards_without_entity_id > 0 THEN
        RAISE NOTICE 'WARNING: % cards without Entity ID found. These may need manual cleanup.', cards_without_entity_id;
    END IF;

    IF orphaned_cards > 0 THEN
        RAISE NOTICE 'WARNING: % orphaned cards found with invalid Entity IDs. These need manual cleanup.', orphaned_cards;
    END IF;

    RAISE NOTICE 'Migration completed successfully. Old organization_id column removed.';
END $$;
