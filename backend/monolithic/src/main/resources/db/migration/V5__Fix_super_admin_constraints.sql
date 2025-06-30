-- V3__Fix_super_admin_constraints.sql
-- Fix the organization_id constraint to allow NULL for Super Admins

-- First, update any existing super admin records to have NULL organization_id
UPDATE entity_admins 
SET organization_id = NULL 
WHERE role_id = (SELECT id FROM roles WHERE name = 'SUPER_ADMIN');

-- Alter the table to allow NULL organization_id
ALTER TABLE entity_admins 
ALTER COLUMN organization_id DROP NOT NULL;

-- Add a check constraint to ensure Entity Admins have an organization
-- but Super Admins can have NULL organization
ALTER TABLE entity_admins 
ADD CONSTRAINT check_entity_admin_has_org 
CHECK (
    (role_id = (SELECT id FROM roles WHERE name = 'SUPER_ADMIN') AND organization_id IS NULL) OR
    (role_id = (SELECT id FROM roles WHERE name = 'ENTITY_ADMIN') AND organization_id IS NOT NULL) OR
    (role_id IS NULL AND organization_id IS NOT NULL)
);

-- Clean up any duplicate super admin entries
DELETE FROM entity_admins 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM entity_admins 
    WHERE username = 'superadmin'
);

-- Ensure we have the correct super admin with proper role
INSERT INTO entity_admins (username, password, organization_id, role_id)
SELECT 'superadmin', '$2a$10$kX6/9Z0SqnX5nGooy2cuglRez2oJ6TP2PzefDs2fzGEGZm4G6dkhO', NULL, r.id
FROM roles r 
WHERE r.name = 'SUPER_ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM entity_admins ea 
    WHERE ea.username = 'superadmin' 
    AND ea.role_id = r.id
);
