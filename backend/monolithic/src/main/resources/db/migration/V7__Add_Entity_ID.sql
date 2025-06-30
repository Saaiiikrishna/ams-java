-- Add entityId column to organizations table (nullable initially)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS entity_id VARCHAR(8);

-- Create unique index for entity_id (only for non-null values)
CREATE UNIQUE INDEX IF NOT EXISTS idx_organizations_entity_id ON organizations(entity_id) WHERE entity_id IS NOT NULL;

-- Note: We don't automatically populate entity_id here to avoid conflicts
-- The migration will be handled by the backend endpoint /super/migrate-entity-ids
-- This allows for better control over which organizations get IDs vs which get removed
