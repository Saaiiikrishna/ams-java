INSERT INTO roles (name) VALUES ('SUPER_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (name) VALUES ('ENTITY_ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO organizations (id, name) VALUES (1, 'Default Org') ON CONFLICT (id) DO NOTHING;
-- password is 'password'
INSERT INTO entity_admins (username, password, organization_id, role_id)
SELECT 'superadmin', '$2a$10$kX6/9Z0SqnX5nGooy2cuglRez2oJ6TP2PzefDs2fzGEGZm4G6dkhO', 1, (SELECT id FROM roles WHERE name='SUPER_ADMIN')
WHERE NOT EXISTS (SELECT 1 FROM entity_admins WHERE username='superadmin');
