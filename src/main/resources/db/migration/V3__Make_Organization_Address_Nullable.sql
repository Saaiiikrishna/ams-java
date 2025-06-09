-- Make the address column in the organizations table nullable
ALTER TABLE organizations
ALTER COLUMN address DROP NOT NULL;
