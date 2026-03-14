-- Seed 'demo1' user with USER role for testing restricted access
-- Password is 'admin123' (same hash as admin)
INSERT INTO users (id, username, email, password, role)
VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    'demo1',
    'demo1@finventory.com',
    '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica',
    'USER'
) ON CONFLICT (username) DO NOTHING;

-- Ensure 'demo' user exists with ADMIN role (if not already present)
-- Password is 'admin123' (same hash as admin) if created new
INSERT INTO users (id, username, email, password, role)
VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
    'demo',
    'demo@finventory.com',
    '$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica',
    'ADMIN'
) ON CONFLICT (username) DO NOTHING;
